package lotos.macros

import shapeless.ReprTypes

import scala.reflect.macros.blackbox

trait SymbolMacros extends ReprTypes {
  val c: blackbox.Context

  import c.internal.{constantType, refinedType}
  import c.universe._

  def taggedType = typeOf[shapeless.tag.Tagged[_]].typeConstructor

  object KeyName {
    def apply(name: String): Type =
      NamedSymbol(appliedType(taggedType, constantType(Constant(name))))

    def unapply(tpe: Type): Option[String] = tpe match {
      case NamedSymbol(ConstantType(Constant(name: String))) => Some(name)
      case ConstantType(Constant(name: String))              => Some(name)
      case _                                                 => None
    }
  }

  object NamedSymbol {
    def apply(tpe: Type): Type = refinedType(List(symbolTpe, tpe), NoSymbol)

    def unapply(tpe: Type): Option[Type] = tpe.dealias match {
      case RefinedType(List(sym, tag, _*), _) if sym == symbolTpe => tag.typeArgs.headOption
      case _                                                      => None
    }
  }

  def freshIdent(name: String): Ident   = Ident(freshName(name))
  def freshName(name: String): TermName = TermName(c.freshName(name))

}
