package lotos.testing

import cats.Parallel
import cats.effect.{Concurrent, ContextShift, IO, Sync}
import cats.implicits._
import lotos.internal.model.{Consistency, PrintLogs, Scenario, SpecT, TestFailure, TestResult, TestSuccess}
import lotos.internal.testing._
import lotos.macros.TestConstructor
import shapeless.HList

object LotosTest {
  def in[F[_]: Concurrent: Parallel]: Runner[F] =
    new Runner[F]

  def forSpec[Impl, Methods <: HList](spec: SpecT[Impl, Methods],
                                      cfg: TestConfig,
                                      consistency: Consistency): IO[TestResult] =
    macro TestConstructor.constructIO[Impl, Methods]

  class Runner[F[_]: Concurrent: Parallel] {
    def forSpec[Impl, Methods <: HList](spec: SpecT[Impl, Methods], cfg: TestConfig, consistency: Consistency)(
        cs: ContextShift[F]): F[TestResult] =
      macro TestConstructor.constructF[F, Impl, Methods]
  }

  def run[F[_]: Concurrent: Parallel](cfg: TestConfig, invoke: Invoke[F], consistency: Consistency)(
      cs: ContextShift[F]): F[TestResult] = {
    val testRun = TestRunImpl(invoke)(cs)
    val scenarios: List[Scenario] =
      List.fill(cfg.scenarioCount)(Scenario.gen(invoke.methods, cfg.parallelism, cfg.scenarioLength))

    scenarios.zipWithIndex
      .map {
        case (scenario, ind) =>
          val iterations = List
            .fill(cfg.scenarioRepetition)(())
            .map(_ =>
              for {
                logs    <- testRun.run(scenario)
                outcome <- lts.sequentially(invoke, logs)
              } yield (logs, outcome))

          for {
            _ <- Sync[F].delay(println(s"Testing scenario ${ind + 1}"))
            scenarioOutcome <- iterations.collectFirstSomeM[F, TestResult](testAction =>
                                testAction.map {
                                  case (_, CheckSuccess(_)) => None
                                  case (logs, CheckFailure) => Some(TestFailure(logs))
                              })
          } yield scenarioOutcome
      }
      .collectFirstSomeM[F, TestResult](identity)
      .flatMap {
        case Some(TestFailure(history)) =>
          Sync[F].delay {
            println("Test failed for scenario:")
            println(PrintLogs.pretty(history))
            TestFailure(history)
          }
        case _ =>
          Sync[F].delay {
            println("Test suceeded")
            TestSuccess
          }
      }
  }
}
