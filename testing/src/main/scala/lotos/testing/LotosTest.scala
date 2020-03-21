package lotos.testing

import cats.Parallel
import cats.effect.{Concurrent, ContextShift, Sync}
import cats.implicits._
import lotos.internal.model.{FuncInvocation, PrintLogs, Scenario, SpecT}
import lotos.internal.testing.{Invoke, TestConfig, TestRunImpl, TestSuccess, lts}
import lotos.macros.RunnerConstructor
import lotos.internal.deepcopy._
import shapeless.HList
import lotos.internal.model.FuncCall

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
      spec <- deepCopyF(invoke)
      logs <- testRun.run(scenario)
      _ = println(PrintLogs.pretty(logs))
      res  <- lts.check(spec, logs)
      _ = res match {
        case TestSuccess(logs) => println(PrintLogs.pretty(List(logs.toList)))
        case other             => println(other)
      }
    } yield ()
  }
}
