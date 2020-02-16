package lotos.macros

import lotos.internal.{Invoker, SpecT}

import scala.reflect.macros.blackbox

class InvokerConstructor(val c: blackbox.Context) extends MacroUtils {

  import c.universe._

  type WTTF[F[_]] = WeakTypeTag[F[Unit]]

  def construct[F[_]: WTTF, Impl: WeakTypeTag](spec: c.Expr[SpecT[Impl]]): c.Expr[Invoker[F]] = {
    val FT    = weakTypeOf[F[Unit]].typeConstructor
    val implT = weakTypeOf[Impl]
    val specT = weakTypeOf[SpecT[Impl]]

    info(specT.decls.toString)
    info(implT.decls.sorted.toString)

    val checkedTree = typeCheckOrAbort(q"""
    new Invoker[$FT] {
      def invoke(method: String): cats.effect.IO[String] = cats.effect.IO("foo")
    }
     """)
    c.Expr(checkedTree)
  }

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

  class CombMatcher(constr: Type) {
    def unapplySeq(t: Type): Option[List[Type]] =
      t baseType constr.typeSymbol match {
        case TypeRef(_, sym, xs) if sym.asType.toType.typeConstructor =:= constr =>
          Some(xs)
        case _ => None
      }
  }

  def symbolName(symbol: Symbol) = symbol.name.decodedName.toString

  def showType(t: Type): String = t.dealias match {
    case TypeRef(_, s, Nil) => symbolName(s)
    case TypeRef(_, s, xs) if xs.nonEmpty =>
      xs.map(showType).mkString(s"${symbolName(s)}[", ",", "]")
  }

  def getPackage(t: Type): Tree =
    t.typeSymbol.fullName
      .split("\\.")
      .foldLeft[Tree](q"_root_") { (pack, name) =>
        q"$pack.${TermName(name)}"
      }
}
