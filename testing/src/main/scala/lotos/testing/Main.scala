package lotos.testing

import lotos.internal.Gen
import lotos.testing.syntax.{method, specFor}
import syntax._
import lotos.internal.Invoker
import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {

  val spec = specFor(new Stack[Int]).methods(
    method("push").paramGen("elem", Gen.intGen),
    method("pop").throws[RuntimeException]
  )

  val inv: Invoker[IO] = LotosTest.invoker[IO](spec)

  override def run(args: List[String]): IO[ExitCode] =
    for {
      res <- inv.invoke("peka")
      _ = println(res)
    } yield ExitCode.Success
}
