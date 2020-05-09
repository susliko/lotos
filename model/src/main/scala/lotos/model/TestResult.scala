package lotos.model

sealed trait TestResult

case object TestSuccess                              extends TestResult
case class TestFailure(history: List[List[TestLog]]) extends TestResult
case class TestCrash(error: Throwable)               extends TestResult
