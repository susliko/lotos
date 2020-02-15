package lotos.testing

import lotos.internal.{Gen, LotosTest}
import lotos.testing.syntax.{method, specFor}
import syntax._

import scala.concurrent.Future

object Main extends App {

  val spec = specFor(new Stack[Int]).methods(
    method("push").paramGen("elem", Gen.intGen),
    method("pop").throws[RuntimeException]
  )

  LotosTest(spec).test[Future]

}
