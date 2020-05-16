package lotos.model

import cats.Eq
import lotos.model.MethodResp.{Fail, Ok, Timeout}

case class TestLog(
    call: MethodCall,
    resp: MethodResp
) {
  def show(callTime: Long, respTime: Long): String = {
    val respShow = resp match {
      case Ok(result, _)  => result
      case Fail(error, _) => error.toString
      case Timeout(_) => "TIMEOUT"
    }
    s"[$callTime; $respTime] ${call.methodName}(${call.params}): $respShow"
  }
}

object TestLog {
  implicit val catsEq: Eq[TestLog] = Eq.instance((a, b) => {
    MethodResp.catsEq.eqv(a.resp, b.resp) && MethodCall.catsEq.eqv(a.call, b.call)
  })
}

sealed trait MethodResp {
  def timestamp: Long
}

object MethodResp {
  case class Ok(result: String, timestamp: Long)     extends MethodResp
  case class Fail(error: Throwable, timestamp: Long) extends MethodResp
  case class Timeout(timestamp: Long) extends MethodResp

  implicit val catsEq: Eq[MethodResp] = Eq.instance((a, b) => {
    (a, b) match {
      case (Ok(resA, _), Ok(resB, _)) if resA == resB                       => true
      case (Fail(errA, _), Fail(errB, _)) if errA.toString == errB.toString => true
      case _                                                                => false
    }
  })
}

case class MethodCall(methodName: String, paramSeeds: Map[String, Long], params: String, timestamp: Long)

object MethodCall {
  implicit val catsEq: Eq[MethodCall] = Eq.instance {
    case (a, b) =>
      a.methodName == b.methodName && a.paramSeeds == b.paramSeeds && a.params == b.params
  }
}

object PrintLogs {
  def pretty(logs: Vector[Vector[TestLog]], scenarioLength: Int): String = {
    val sortedTimes = logs
      .flatMap(_.flatMap(event => List(event.call.timestamp, event.resp.timestamp)))
      .sorted
      .zipWithIndex

    val maxTime = sortedTimes.length

    val timesMapping =
      sortedTimes.toMap
        .mapValues(_.toLong)

    val maxLogLength = logs.flatMap(_.map(_.show(maxTime, maxTime).length)).max
    val emptyLog = " " * (maxLogLength + 2)
    logs
      .map { column =>
        column
          .map(event => {
            val str =
              event.show(timesMapping(event.call.timestamp), timesMapping(event.resp.timestamp))
            s""" $str${" " * (maxLogLength - str.length)} """
          })
      }
      .fold(Vector.fill(scenarioLength)("")) {
        case (l1, l2) => l1.zipAll(l2, emptyLog, emptyLog).map{case (s1, s2) => s"$s1|$s2"}
      }
      .mkString("\n")
  }
}
