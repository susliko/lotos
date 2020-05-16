package lotos.model

import scala.concurrent.duration._

case class TestConfig(parallelism: Int = 2,
                      scenarioLength: Int = 10,
                      scenarioRepetition: Int = 100,
                      scenarioCount: Int = 100,
                      operationTimeout: FiniteDuration = 1.second)
