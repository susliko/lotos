package lotos.macros
import scala.reflect.NameTransformer
import scala.reflect.macros.{TypecheckException, blackbox}

trait MacroUtils {
  val c: blackbox.Context

  import c.universe._

  def abort(s: String) = c.abort(c.enclosingPosition, s)
  def info(s: String)  = c.info(c.enclosingPosition, s, force = true)

  def typeCheckOrAbort(t: Tree): Tree =
    try c.typecheck(t)
    catch {
      case ex: TypecheckException => c.abort(c.enclosingPosition, ex.toString)
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

  def unpackString(sType: Type): String =
    sType match {
      case ConstantType(Constant(s: String)) => NameTransformer.encode(s)
      case x                                 => abort(s"$x should be a string constant")
    }

  def symbolName(symbol: Symbol): String = symbol.name.decodedName.toString
}
