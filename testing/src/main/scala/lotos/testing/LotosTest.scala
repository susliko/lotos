package lotos.testing

import java.util.concurrent.Executors

import cats.Parallel
import cats.implicits._
import cats.effect.{Concurrent, ContextShift, IO, Resource, Sync}
import lotos.internal.model.{PrintLogs, Scenario, SpecT}
import lotos.internal.testing.{Invoke, TestRunImpl}
import lotos.macros.InvokeConstructor
import shapeless.HList

import scala.concurrent.ExecutionContext

object LotosTest {
  def genInvoke[F[_]: Sync] = new Applier[F]

  class Applier[F[_]: Sync] {
    def forSpec[Impl, Methods <: HList](spec: SpecT[Impl, Methods]): Invoke[F] =
      macro InvokeConstructor.construct[F, Impl, Methods]
  }

  def run[F[_]: Concurrent: Parallel](cfg: TestConfig, invoke: Invoke[F])(cs: ContextShift[F]): F[Unit] = {
    val testRun            = TestRunImpl(invoke)(cs)
    val scenario: Scenario = Scenario.gen(invoke.methods, cfg.parallelism, cfg.length)
    for {
      logs <- testRun.run(scenario)
      _    = println(PrintLogs.pretty(logs))
    } yield ()
  }

  def runIO(cfg: TestConfig, invoke: Invoke[IO]): IO[Unit] =
    Resource
      .make(IO(Executors.newFixedThreadPool(cfg.parallelism)))(ex => IO(ex.shutdown()))
      .map(ex => IO.contextShift(ExecutionContext.fromExecutor(ex)))
      .use(implicit cs => run(cfg, invoke)(cs))
}
