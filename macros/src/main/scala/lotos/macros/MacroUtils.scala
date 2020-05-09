package lotos.macros
import scala.reflect.macros.{TypecheckException, blackbox}

trait MacroUtils {
  val c: blackbox.Context

  import c.universe._

  def abort(s: String) = c.abort(c.enclosingPosition, s)
  def info(s: String)  = c.info(c.enclosingPosition, s, force = true)

  def inferImplicitOrAbort(t: Type): Tree =
    try c.inferImplicitValue(pt = t, silent = false)
    catch {
      case _: TypecheckException => c.abort(c.enclosingPosition, s"could not find implicit value for ${t.dealias.toString}")
    }
  def typeCheckOrAbort(t: Tree): Tree =
    try c.typecheck(t)
    catch {
      case ex: TypecheckException => c.abort(c.enclosingPosition, ex.toString)
    }
}
