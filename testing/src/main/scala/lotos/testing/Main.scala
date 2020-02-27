package lotos.testing

import cats.effect.{ExitCode, IO, IOApp}
import lotos.internal.model.Gen
import lotos.internal.testing.TestConfig
import lotos.testing.syntax.{method, spec}

/*_*/
object Main extends IOApp {
  val stackSpec =
    spec(new Stack[Int])
      .withMethod(method("push").param("elem")(Gen.intGen))
      .withMethod(method("pop").throws[RuntimeException])

  val cfg = TestConfig(2, 10)

  override def run(args: List[String]): IO[ExitCode] =
    for {
      runner <- LotosTest.inIO(cfg.parallelism)
      _      <- runner.forSpec(stackSpec, cfg)
    } yield ExitCode.Success

}
