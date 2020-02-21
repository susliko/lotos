package lotos.macros

import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._

import lotos.internal.model.{MethodT, SpecT, TestedImpl}
import shapeless.{HList, HNil}

import scala.reflect.NameTransformer
import scala.reflect.macros.blackbox

class TestedImplConstructor(val c: blackbox.Context) extends ShapelessMacros {
  import c.universe._

  type WTTF[F[_]] = WeakTypeTag[F[Unit]]

  def construct[F[_]: WTTF, Impl: WeakTypeTag, Methods <: HList: WeakTypeTag](
      spec: c.Expr[SpecT[Impl, Methods]]): c.Expr[TestedImpl[F]] = {
    val FT      = weakTypeOf[F[Unit]].typeConstructor
    val methodT = weakTypeOf[MethodT[Unit, HNil, HNil]].typeConstructor

    val implT = weakTypeOf[Impl]
    println(implT.decls)
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

    println(implMethods)
    checkSpecOrAbort(specMethods, implMethods)

    val methodMatch = specMethods.map {
      case (mName, params) =>
        val paramList = params.map {
          case (pName, tpe) =>
            q"""${TermName(pName)} = paramGens(${q"$pName"}).asInstanceOf[Gen[$tpe]].gen"""
        }
        println(paramList)
        val invocation =
          if (paramList.isEmpty)
            q"Sync[$FT].delay(impl.${TermName(mName)}.toString)"
          else
            q"Sync[$FT].delay(impl.${TermName(mName)}(..$paramList).toString)"
        cq"""${q"$mName"} =>
            val methodT =
                SpecT.methods($spec)(method)
                     .asInstanceOf[MethodT[..${methodTypeParams(mName)}]]
            val paramGens = MethodT.paramGens(methodT)
            $invocation"""
    } :+ cq"""_ => Monad[$FT].pure("Unknown method") """

    println(methodMatch)
    val checkedTree = typeCheckOrAbort(q"""
    import lotos.internal.model.{SpecT, MethodT, Gen}
    import cats.effect.Sync
    import cats.Monad

    new TestedImpl[$FT] {
      val impl = SpecT.construct($spec)

      def copy: TestedImpl[$FT] = this
      def invoke(method: String): ${appliedType(FT, typeOf[String])} = {
        method match {
            case ..$methodMatch
        }
      }
    }
     """)
    println(checkedTree)
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
