package lotos.testing

import cats.effect.{ExitCode, IO, IOApp}
import lotos.internal.model.Gen
import lotos.testing.syntax.{method, spec}

/*_*/
object Main extends IOApp {
  val stackSpec =
    spec(new Stack[Int])
      .withMethod(method("push").param("elem")(Gen.intGen))
      .withMethod(method("pop").throws[RuntimeException])

  override def run(args: List[String]): IO[ExitCode] =
    for {
      invoke <- IO(LotosTest.genInvoke[IO].forSpec(stackSpec))
      _      <- LotosTest.runIO(TestConfig(2, 10), invoke)
    } yield ExitCode.Success

}
