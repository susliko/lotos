package lotos.model

case class TestConfig(parallelism: Int = 2,
                      scenarioLength: Int = 10,
                      scenarioRepetition: Int = 100,
                      scenarioCount: Int = 100)
