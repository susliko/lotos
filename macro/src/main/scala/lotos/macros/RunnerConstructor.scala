package lotos.macros

import cats.effect.{ContextShift, Sync}
import cats.instances.either._
import cats.instances.list._
import cats.syntax.traverse._
import lotos.internal.model._
import lotos.internal.testing.TestConfig
import shapeless.{HList, HNil}

import scala.reflect.NameTransformer
import scala.reflect.macros.blackbox

class RunnerConstructor(val c: blackbox.Context) extends ShapelessMacros {
  import c.universe._

  type WTTF[F[_]] = WeakTypeTag[F[Unit]]

  def construct[F[_]: WTTF, Impl: WeakTypeTag, Methods <: HList: WeakTypeTag](
      spec: c.Expr[SpecT[Impl, Methods]],
      cfg: c.Expr[TestConfig])(cs: c.Expr[ContextShift[F]]): c.Expr[F[Unit]] = {
    val FT      = weakTypeOf[F[Unit]].typeConstructor
    val methodT = weakTypeOf[MethodT[Unit, HNil, HNil]].typeConstructor

    val syncF = inferImplicitOrAbort(
      appliedType(
        weakTypeOf[Sync[F]].typeConstructor,
        FT
      )
    )

    val implT = weakTypeOf[Impl]
    val specT = weakTypeOf[Methods]

    val implMethods = extractMethods(implT).toMap
    val specMethods: List[(String, NList[Type])] = hlistElements(specT).collect {
      case method if method.typeConstructor == methodT =>
        method.typeArgs match {
          case List(name, params, _) => (unpackString(name), extractRecord(params))
          case _                     => abort(s"unexpected method definition $method")
        }
    }
    val methodTypeParams: Map[String, List[Type]] = hlistElements(specT).collect {
      case method if method.typeConstructor == methodT =>
        method.typeArgs match {
          case List(name, params, errors) => (unpackString(name), List(name, params, errors))
          case _                          => abort(s"unexpected method definition $method")
        }
    }.toMap

    checkSpecOrAbort(specMethods, implMethods)

    val methodMatch = specMethods.map {
      case (mName, params) =>
        val paramList = params.map {
          case (pName, tpe) =>
            q"""{
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
        cq"""${q"$mName"} =>
            val seeds: Map[String, Long] = Map(..${params.map { case (pName, _) => q"$pName -> random.nextLong()" }})
            var showParams: Map[String, String] = Map.empty
            val methodT =
                SpecT.methods($spec)(method)
                     .asInstanceOf[MethodT[..${methodTypeParams(mName)}]]
            val paramGens = MethodT.paramGens(methodT)
            $invocation.map { res =>
              List(
               FuncInvocation(${q"$mName"},
                        seeds,
                        showParams.toList.map{case (k, v) => k + " = " + v}.mkString(", "),
                        res.toString))
            }
          """
    } :+ cq"""_ => $syncF.pure(List(FuncCall("Unknown method", Map.empty, ""))) """

    val checkedTree = typeCheckOrAbort(q"""
    import lotos.internal.model._

    import lotos.internal.testing._
    import scala.util.Random

    val invoke = new Invoke[$FT] {
      private val random = new Random(System.currentTimeMillis())
      private val impl = SpecT.construct($spec)

      def copy: Invoke[$FT] = this
      def invoke(method: String): ${appliedType(FT, typeOf[List[LogEvent]])} = {
        method match {
            case ..$methodMatch
        }
      }
      def methods: List[String] = ${specMethods.map(_._1)}
    }
    LotosTest.run($cfg, invoke)($cs)
     """)
    c.Expr(checkedTree)
  }

  def checkSpecOrAbort(specMethods: List[(String, List[(String, Type)])],
                       implMethods: Map[String, MethodDecl[Type]]): Unit = {
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
                                      s"argument `$implArgName` of method `$name` does not exist in the specification")
                    _ <- Either.cond(
                          specArgType =:= implArgType,
                          (),
                          s"argument `$implArgName` of method `$name` is declared to be `$specArgType` but `$implArgType` encountered in implementation",
                        )
                  } yield (implArgName, implArgType)
              }
        } yield ()
        validation.fold(abort, identity)
    }
  }

  def extractMethods(tpe: Type): NList[MethodDecl[Type]] =
    tpe.decls.collect {
      case s: MethodSymbol =>
        symbolName(s) ->
          (s.infoIn(tpe)
            .paramLists
            .map(lst => lst.map(p => symbolName(p) -> p.typeSignature)) ->
            s.infoIn(tpe).resultType)
    }.toList

  def extractMeth(typ: Type, name: Name): Option[MethodSymbol] =
    typ.decl(name) match {
      case ms: MethodSymbol                 => Some(ms)
      case ov if ov.alternatives.length > 1 => abort("could not handle method overloading")
      case _ =>
        typ.baseClasses.tail.iterator
          .collect {
            case base: TypeSymbol if base != typ => base.toType
          }
          .flatMap {
            extractMeth(_, name)
          }
          .collectFirst {
            case x => x
          }
    }

  def findMeth(typ: Type, group: Vector[String], name: Name): Option[MethodSymbol] =
    group match {
      case first +: rest =>
        typ.decl(TermName(first)) match {
          case ms: MethodSymbol if ms.paramLists == Nil => findMeth(ms.typeSignature, rest, name)
          case ms: MethodSymbol                         => abort(s"group $first is a method with parameters : ${ms.paramLists}")
          case ms: ModuleSymbol                         => findMeth(ms.typeSignature, rest, name)
          case _                                        => None
        }
      case Vector() => extractMeth(typ, name)
    }

  def unpackString(sType: Type): String =
    sType match {
      case ConstantType(Constant(s: String)) => NameTransformer.encode(s)
      case x                                 => abort(s"$x should be a string constant")
    }

  def symbolName(symbol: Symbol) = symbol.name.decodedName.toString

}
