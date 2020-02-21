package lotos.testing

import cats.effect.{ExitCode, IO, IOApp}
import lotos.internal.model.{Gen, TestedImpl}
import lotos.testing.syntax.{method, spec}

/*_*/
object Main extends IOApp {
  val stackSpec =
    spec(new Stack[Int])
      .withMethod(method("push").paramGen("elem", Gen.intGen))
      .withMethod(method("pop").throws[RuntimeException])

  val inv: TestedImpl[IO] = LotosTest[IO].forSpec(stackSpec)

  override def run(args: List[String]): IO[ExitCode] =
    for {
      res <- inv.invoke("push")
      res <- inv.invoke("pop")
      _   = println(res)
    } yield ExitCode.Success
}
