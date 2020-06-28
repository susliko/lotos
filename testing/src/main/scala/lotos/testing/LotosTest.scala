package lotos.testing

import java.nio.file.Paths

import cats.Parallel
import cats.effect.{Concurrent, ContextShift, IO, Sync, Timer}
import cats.implicits._
import lotos.model._
import lotos.internal.testing._
import lotos.internal.testing.lts._
import lotos.macros.TestConstructor
import shapeless.HList

object LotosTest {
  def in[F[_]]: Runner[F] =
    new Runner[F]

  def forSpec[Impl, Methods <: HList](spec: SpecT[Impl, Methods], cfg: TestConfig, consistency: Consistency)(
      implicit timer: Timer[IO]
  ): IO[TestResult] =
    macro TestConstructor.constructIO[Impl, Methods]

  class Runner[F[_]] {
    def forSpec[Impl, Methods <: HList](spec: SpecT[Impl, Methods], cfg: TestConfig, consistency: Consistency)(
        cs: ContextShift[F]
    )(implicit F: Concurrent[F], timer: Timer[F]): F[TestResult] =
      macro TestConstructor.constructF[F, Impl, Methods]
  }

  def run[F[_]: Parallel: Timer](cfg: TestConfig, invoke: Invoke[F], consistency: Consistency)(
      cs: ContextShift[F]
  )(implicit F: Concurrent[F]): F[TestResult] = {
    val testRun = TestRunImpl(invoke)(cs)
    val scenarios: List[Scenario] =
      List.fill(cfg.scenarioCount)(Scenario.gen(invoke.methods, cfg.parallelism, cfg.scenarioLength))

    scenarios.zipWithIndex
      .map {
        case (scenario, ind) =>
          val iterations = List
            .fill(cfg.scenarioRepetition) {
              for {
                logs <- testRun.run(scenario, cfg.operationTimeout)
                outcome <- if (logs.exists(_.length != cfg.scenarioLength)) (CheckNotStarted: CheckResult).pure[F]
                          else
                            consistency match {
                              case Consistency.sequential   => lts.sequentially(invoke, logs)
                              case Consistency.linearizable => lts.linearizable(invoke, logs)
                            }
              } yield (logs, outcome)
            }

          for {
            _ <- F.delay(println(s"Testing scenario ${ind + 1}"))
            scenarioOutcome <- iterations.collectFirstSomeM[F, TestResult](testAction =>
                                testAction.map {
                                  case (_, CheckSuccess(_))    => None
                                  case (logs, CheckFailure)    => Some(TestFailure(logs))
                                  case (logs, CheckNotStarted) => Some(TestTimeout(logs))
                                }
                              )
          } yield scenarioOutcome
      }
      .collectFirstSomeM[F, TestResult](identity)
      .flatMap {
        case Some(failure @ TestFailure(history)) =>
          F.delay(println("Test failed")) *> onFailure(history, cfg).as(failure)
        case Some(crash @ TestTimeout(history)) =>
          F.delay(println(s"Test timeouted")) *> onFailure(history, cfg).as(crash)
        case _ =>
          F.delay(println("Test succeeded")).as(TestSuccess)
      }
  }

  def onFailure[F[_]](logs: Vector[Vector[TestLog]], cfg: TestConfig)(implicit F: Sync[F]): F[Unit] =
    cfg.failureFormat match {
      case FailureFormat.Console => F.delay(println(PrintLogs.prettyString(logs, cfg.scenarioLength)))
      case FailureFormat.Diagram(fileName) =>
        F.delay(println(s"Writing logs to ${Paths.get(fileName).toAbsolutePath}")) *> PrintLogs
          .diagram[F](logs, cfg.scenarioLength, fileName)
      case FailureFormat.Both(fileName) =>
        for {
          _ <- F.delay(println(s"Writing logs to $fileName"))
          _ <- F.delay(println(PrintLogs.prettyString(logs, cfg.scenarioLength)))
          _ <- PrintLogs.diagram[F](logs, cfg.scenarioLength, fileName)
        } yield ()
    }
}
