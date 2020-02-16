package lotos.macros
import scala.reflect.macros.{TypecheckException, blackbox}

trait MacroUtils {
  val c: blackbox.Context

  def abort(s: String) = c.abort(c.enclosingPosition, s)
  def info(s: String)  = c.info(c.enclosingPosition, s, force = true)

  def typeCheckOrAbort(t: c.Tree): c.Tree =
    try (c.typecheck(t))
    catch {
      case ex: TypecheckException => c.abort(c.enclosingPosition, ex.toString)
    }
}
