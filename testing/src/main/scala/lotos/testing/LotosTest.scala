package lotos.testing

import java.util.concurrent.Executors

import cats.Parallel
import cats.effect.{Concurrent, ContextShift, IO, Resource}
import cats.implicits._
import lotos.internal.model.{PrintLogs, Scenario, SpecT}
import lotos.internal.testing.{Invoke, TestConfig, TestRunImpl}
import lotos.macros.RunnerConstructor
import shapeless.HList

import scala.concurrent.ExecutionContext

object LotosTest {
  def inF[F[_]: Concurrent: Parallel: ContextShift] = new Runner[F]

  def inIO(parallelism: Int): IO[Runner[IO]] =
    Resource
      .make(IO(Executors.newFixedThreadPool(parallelism)))(ex => IO(ex.shutdown()))
      .map(ex => IO.contextShift(ExecutionContext.fromExecutor(ex)))
      .use(implicit cs => IO(new Runner[IO]))

  class Runner[F[_]: Concurrent: Parallel: ContextShift] {
    def forSpec[Impl, Methods <: HList](spec: SpecT[Impl, Methods], cfg: TestConfig): F[Unit] =
      macro RunnerConstructor.construct[F, Impl, Methods]
  }

  def run[F[_]: Concurrent: Parallel](cfg: TestConfig, invoke: Invoke[F])(implicit cs: ContextShift[F]): F[Unit] = {
    val testRun            = TestRunImpl(invoke)(cs)
    val scenario: Scenario = Scenario.gen(invoke.methods, cfg.parallelism, cfg.length)
    for {
      logs <- testRun.run(scenario)
      _    = println(PrintLogs.pretty(logs))
    } yield ()
  }
}
