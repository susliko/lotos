package lotos.macros

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
    val specT = weakTypeOf[Methods]

    val implMethods = extractMethods(implT).toMap
    val specMethods = hlistElements(specT).collect {
      case method if method.typeConstructor == methodT =>
        method.typeArgs match {
          case List(name, params, errors) => (unpackString(name), extractRecord(params))
          case _                          => abort(s"unexpected method definition $method")
        }
    }
    println(specMethods)
    println(implMethods)
    val checkedTree = typeCheckOrAbort(q"""
    new TestedImpl[$FT] {
      val impl = $spec.construct()

      def copy: TestedImpl[$FT] = this
      def invoke(method: String): ${appliedType(FT, typeOf[String])} = {
       cats.Monad[$FT].pure("foo")
      }
    }
     """)
    c.Expr(checkedTree)
  }

  def extractMethods(tpe: Type): NList[(List[NList[Type]], Type)] =
    tpe.decls.collect {
      case s: MethodSymbol =>
        symbolName(s) ->
          (s.paramLists.map(lst => lst.map(p => symbolName(p) -> p.typeSignature)) -> s.returnType)
    }.toList

  def extractMethod(meth: MethodSymbol): MethodDecl[Type] =
    meth.paramLists.map(lst => lst.map(p => symbolName(p) -> p.typeSignature)) -> meth.returnType

  private def extractMeth(typ: Type, name: Name): Option[MethodSymbol] =
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

  private def findMeth(typ: Type, group: Vector[String], name: Name): Option[MethodSymbol] =
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
      case x                            => abort(s"$x should be a string constant")
    }

  def symbolName(symbol: Symbol) = symbol.name.decodedName.toString

}
