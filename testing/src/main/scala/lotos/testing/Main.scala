package lotos.testing

import java.util.concurrent.Executors

import cats.effect.{ContextShift, ExitCode, IO, IOApp}
import lotos.internal.model.{Gen, PrintLogs, Scenario}
import lotos.internal.testing.{Invoke, TestRun, TestRunImpl}
import lotos.testing.syntax.{method, spec}
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

/*_*/
object Main extends IOApp {
  val stackSpec =
    spec(new Stack[Int])
      .withMethod(method("push").param("elem")(Gen.intGen))
      .withMethod(method("pop").throws[RuntimeException])

  val invoke: Invoke[IO] = LotosTest[IO].forSpec(stackSpec)
  val scenario: Scenario = Scenario.gen(invoke.methods, 2, 50)

  val executor = Executors.newFixedThreadPool(2)
  val ec: ExecutionContext = ExecutionContext.fromExecutor(executor)
  val cs: ContextShift[IO] = IO.contextShift(global)
  val testRun: TestRun[IO] = TestRunImpl[IO](invoke)(cs)

  override def run(args: List[String]): IO[ExitCode] =
    for {
      logs <- testRun.run(scenario)
      _    = println(PrintLogs.pretty(logs))
      _ = executor.shutdown()
    } yield ExitCode.Success

}
