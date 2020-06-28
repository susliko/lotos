package lotos.model

import scala.concurrent.duration._

sealed trait FailureFormat

object FailureFormat {
  case object Console extends FailureFormat
  case class Diagram(fileName: String) extends FailureFormat
  case class Both(fileName: String) extends FailureFormat
}

case class TestConfig(
    parallelism: Int = 2,
    scenarioLength: Int = 10,
    scenarioRepetition: Int = 100,
    scenarioCount: Int = 100,
    operationTimeout: FiniteDuration = 1.second,
    failureFormat: FailureFormat = FailureFormat.Console
)
