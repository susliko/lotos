package lotos.macros

import cats.effect.{Concurrent, ContextShift, IO, Sync}
import cats.instances.either._
import cats.instances.list._
import cats.syntax.traverse._
import lotos.model._
import lotos.internal.testing.Invoke
import lotos.model.TestResult
import shapeless.{HList, HNil}

import scala.reflect.NameTransformer
import scala.reflect.macros.blackbox

class TestConstructor(val c: blackbox.Context) extends ShapelessMacros {
  import c.universe._

  type WTTF[F[_]] = WeakTypeTag[F[Unit]]

  def constructF[F[_]: WTTF, Impl: WeakTypeTag, Methods <: HList: WeakTypeTag](
      spec: c.Expr[SpecT[Impl, Methods]],
      cfg: c.Expr[TestConfig],
      consistency: c.Expr[Consistency]
  )(cs: c.Expr[ContextShift[F]])(F: c.Expr[Concurrent[F]]): c.Expr[F[TestResult]] = {
    val invoke = constructInvoke[F, Impl, Methods](spec)(F)

    val testRunTree = q"lotos.testing.LotosTest.run($cfg, $invoke, $consistency)($cs)"
    val checkedTree = typeCheckOrAbort(testRunTree)
    c.Expr(checkedTree)
  }

  def constructIO[Impl: WeakTypeTag, Methods <: HList: WeakTypeTag](
      spec: c.Expr[SpecT[Impl, Methods]],
      cfg: c.Expr[TestConfig],
      consistency: c.Expr[Consistency]
  ): c.Expr[IO[TestResult]] = {
    val invoke      = constructInvoke[IO, Impl, Methods](spec)(c.Expr(q"cats.effect.IO.ioEffect"))
    val testRunTree = q"""
      import cats.effect.{ContextShift, Resource}
      import scala.concurrent.ExecutionContext
      import java.util.concurrent.Executors
      
      val csResource = Resource
       .make(IO(Executors.newFixedThreadPool($cfg.parallelism)))(ex => IO(ex.shutdown()))
       .map(ex => IO.contextShift(ExecutionContext.fromExecutor(ex)))
       
      csResource.use(cs => lotos.testing.LotosTest.run($cfg, $invoke, $consistency)(cs))
      """
    val checkedTree = typeCheckOrAbort(testRunTree)
    c.Expr(checkedTree)
  }

  private def constructInvoke[F[_]: WTTF, Impl: WeakTypeTag, Methods <: HList: WeakTypeTag](
      spec: c.Expr[SpecT[Impl, Methods]],
  )(syncF: c.Expr[Sync[F]]): c.Expr[Invoke[F]] = {
    val FT      = weakTypeOf[F[Unit]].typeConstructor
    val methodT = weakTypeOf[MethodT[Unit, HNil]].typeConstructor

    val implT = weakTypeOf[Impl]
    val specT = weakTypeOf[Methods]

    val implMethods = extractMethods(implT).toMap
    val specMethods: List[(String, NList[Type])] = hlistElements(specT).collect {
      case method if method.typeConstructor == methodT =>
        method.typeArgs match {
          case List(name, params) => (unpackString(name), extractRecord(params))
          case _                  => abort(s"unexpected method definition $method")
        }
    }
    val methodTypeParams: Map[String, List[Type]] = hlistElements(specT).collect {
      case method if method.typeConstructor == methodT =>
        method.typeArgs match {
          case List(name, params) => (unpackString(name), List(name, params))
          case _                  => abort(s"unexpected method definition $method")
        }
    }.toMap

    checkSpecOrAbort(specMethods, implMethods)

    def methodMatch(withSeeds: Boolean) =
      specMethods.map {
        case (mName, reversedParams) =>
          val params = reversedParams.reverse
          val paramList = params.map {
            case (pName, tpe) =>
              q"""${TermName(pName)} = {
               val paramGen = paramGens($pName).asInstanceOf[Gen[$tpe]]
               val param = paramGen.gen(seeds($pName))
               showParams = showParams + ($pName -> paramGen.show(param))
               param
             }"""
          }
          val invocation =
            if (paramList.isEmpty)
              q"$syncF.delay(impl.${TermName(mName)}.toString)"
            else
              q"$syncF.delay(impl.${TermName(mName)}(..$paramList).toString)"

          val seeds =
            if (withSeeds) q""
            else
              q"""val seeds: Map[String, Long] = Map(..${params.map {
                case (pName, _) => q"$pName -> random.nextLong()"
              }})"""

          cq"""${q"$mName"} =>
            $seeds
            var showParams: Map[String, String] = Map.empty
            val methodT =
                SpecT.methods($spec)(method)
                     .asInstanceOf[MethodT[..${methodTypeParams(mName)}]]
            val paramGens = MethodT.paramGens(methodT)
            for {
              startTime <- $syncF.delay(System.nanoTime)
              resp <- $invocation.flatMap(okResp =>
                        $syncF.delay(System.nanoTime).map(endTime =>
                          MethodResp.Ok(
                            result = okResp.toString,
                            timestamp = endTime
                          )))
                        .handleErrorWith(error =>
                        $syncF.delay(System.nanoTime).map(endTime =>
                           MethodResp.Fail(
                            error = error,
                            timestamp = endTime
                        )))
            } yield TestLog(
                      call = MethodCall(
                        methodName = ${q"$mName"},
                        paramSeeds = seeds,
                        params = showParams.toList.map{case (k, v) => k + "=" + v}.mkString(", "),
                        timestamp = startTime
                      ),
                      resp = resp)
          """
      } :+ cq"""_ => $syncF.pure(TestLog(MethodCall("Unknown method", Map.empty, "", 0L), MethodResp.Ok("",0L))) """

    val invokeTree  = q"""
      import lotos.model._

      import lotos.internal.testing._
      import scala.util.Random

      new Invoke[$FT] {
        private val random = new Random(System.currentTimeMillis())
        private val impl = SpecT.construct($spec)

        def copy: Invoke[$FT] = this
        def invoke(method: String): ${appliedType(FT, typeOf[TestLog])} =
          method match {
              case ..${methodMatch(withSeeds = false)}
          }
        def invokeWithSeeds(method: String, seeds: Map[String, Long]): ${appliedType(FT, typeOf[TestLog])} =
          method match {
            case ..${methodMatch(withSeeds = true)}
          }
        def methods: List[String] = ${specMethods.map(_._1)}
      }
     """
    val checkedTree = typeCheckOrAbort(invokeTree)
    c.Expr(checkedTree)
  }

  private def checkSpecOrAbort(
      specMethods: List[(String, List[(String, Type)])],
      implMethods: Map[String, MethodDecl[Type]]
  ): Unit = {
    specMethods.foreach {
      case (name, args) =>
        val specArgsMap = args.toMap
        val validation = for {
          (implArgLists, _) <- implMethods
                                .get(name)
                                .toRight(s"specified method `$name` does not exist in the implementation")
          _ <- implArgLists.flatten.traverse {
                case (implArgName, implArgType) =>
                  for {
                    specArgType <- specArgsMap
                                    .get(implArgName)
                                    .toRight(
                                      s"argument `$implArgName` of method `$name` does not exist in the specification"
                                    )
                    _ <- Either.cond(
                          specArgType =:= implArgType,
                          (),
                          s"argument `$implArgName` of method `$name` is declared to be `$specArgType` but `$implArgType` encountered in implementation"
                        )
                  } yield (implArgName, implArgType)
              }
        } yield ()
        validation.fold(abort, identity)
    }
  }
}
