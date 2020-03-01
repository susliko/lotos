package lotos.testing

import cats.Parallel
import cats.effect.{Concurrent, ContextShift}
import cats.implicits._
import lotos.internal.model.{PrintLogs, Scenario, SpecT}
import lotos.internal.testing.{Invoke, TestConfig, TestRunImpl}
import lotos.macros.RunnerConstructor
import shapeless.HList

object LotosTest {
  def apply[F[_]: Concurrent: Parallel]: Runner[F] =
    new Runner[F]

  class Runner[F[_]: Concurrent: Parallel] {
    def forSpec[Impl, Methods <: HList](spec: SpecT[Impl, Methods], cfg: TestConfig)(cs: ContextShift[F]): F[Unit] =
      macro RunnerConstructor.construct[F, Impl, Methods]
  }

  def run[F[_]: Concurrent: Parallel](cfg: TestConfig, invoke: Invoke[F])(cs: ContextShift[F]): F[Unit] = {
    val testRun            = TestRunImpl(invoke)(cs)
    val scenario: Scenario = Scenario.gen(invoke.methods, cfg.parallelism, cfg.length)
    for {
      logs <- testRun.run(scenario)
      _    = println(PrintLogs.pretty(logs))
    } yield ()
  }
}
