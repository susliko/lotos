package lotos.testing

import java.util.concurrent.Executors

import cats.effect.{ContextShift, ExitCode, IO, IOApp, Resource}
import lotos.internal.model.Gen
import lotos.internal.testing.TestConfig
import lotos.testing.syntax.{method, spec}

import scala.concurrent.ExecutionContext

/*_*/
object Main extends IOApp {
  val stackSpec =
    spec(new Stack[Int])
      .withMethod(method("push").param("elem")(Gen.intGen))
      .withMethod(method("pop").throws[RuntimeException])

  val cfg = TestConfig(parallelism = 2, length = 10)

  def mkContextShift(parallelism: Int): Resource[IO, ContextShift[IO]] =
    Resource
      .make(IO(Executors.newFixedThreadPool(parallelism)))(ex => IO(ex.shutdown()))
      .map(ex => IO.contextShift(ExecutionContext.fromExecutor(ex)))

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- mkContextShift(cfg.parallelism).use(cs => LotosTest[IO].forSpec(stackSpec, cfg)(cs))
    } yield ExitCode.Success

}
