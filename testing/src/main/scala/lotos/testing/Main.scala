package lotos.testing

import java.util.concurrent.Executors

import cats.effect.{ContextShift, ExitCode, IO, IOApp}
import lotos.internal.model.{Gen, PrintLogs, Scenario}
import lotos.internal.testing.{Invoke, TestRun, TestRunImpl}
import lotos.testing.syntax.{method, spec}

import scala.concurrent.ExecutionContext

/*_*/
object Main extends IOApp {
  val stackSpec =
    spec(new Stack[Int])
      .withMethod(method("push").param("elem")(Gen.intGen))
      .withMethod(method("pop").throws[RuntimeException])

  val invoke: Invoke[IO] = LotosTest[IO].forSpec(stackSpec)
  val scenario: Scenario = Scenario.gen(invoke.methods, 2, 5)

  val ec                   = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
  val cs: ContextShift[IO] = IO.contextShift(ec)
  val testRun: TestRun[IO] = TestRunImpl[IO](invoke)(cs)

  override def run(args: List[String]): IO[ExitCode] =
    for {
      logs <- testRun.run(scenario)
      _    = println(PrintLogs.pretty(logs))
    } yield ExitCode.Success

}
