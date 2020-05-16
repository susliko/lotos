package lotos.model

sealed trait TestResult

case object TestSuccess                                  extends TestResult
case class TestFailure(history: Vector[Vector[TestLog]]) extends TestResult
case class TestTimeout(history: Vector[Vector[TestLog]]) extends TestResult
