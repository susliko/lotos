package lotos.testing

import cats.effect.{ExitCode, IO, IOApp}
import lotos.internal.model.{Gen, Invoker, PrintLogs}
import lotos.testing.syntax.{method, spec}

/*_*/
object Main extends IOApp {
  val stackSpec =
    spec(new Stack[Int])
      .withMethod(method("push").param("elem")(Gen.intGen))
      .withMethod(method("pop").throws[RuntimeException])

  val inv: Invoker[IO] = LotosTest[IO].forSpec(stackSpec)

  override def run(args: List[String]): IO[ExitCode] =
    for {
      (r1b, r1e) <- inv.invoke("push")
      (r2b, r2e) <- inv.invoke("pop")
      _  = println(PrintLogs.pretty(List(List(r1b, r1e), List(r2b, r2e))))
    } yield ExitCode.Success

}
