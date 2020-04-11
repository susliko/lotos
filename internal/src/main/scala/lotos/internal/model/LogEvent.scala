package lotos.internal.model

import cats.Eq

trait LogEvent { self =>
  def methodName: String
  def show: String
  def paramSeeds: Map[String, Long]
}

object LogEvent {
  implicit val catsEq: Eq[LogEvent] = Eq.instance {
    case (a: FuncCall, b: FuncCall) =>
      FuncCall.catsEq.eqv(a, b)
    case (a: FuncResp, b: FuncResp) =>
      FuncResp.catsEq.eqv(a, b)
    case (a: FuncInvocation, b: FuncInvocation) =>
      FuncInvocation.catsEq.eqv(a, b)
    case _ =>
      false
  }
}

case class FuncCall(methodName: String, paramSeeds: Map[String, Long], showParams: String, timestamp: Long)
    extends LogEvent {
  def show: String = s"$methodName($showParams)"
}

object FuncCall {
  implicit val catsEq: Eq[FuncCall] = Eq.instance {
    case (a, b) =>
      a.methodName == b.methodName && a.paramSeeds == b.paramSeeds && a.showParams == b.showParams
  }
}

case class FuncResp(methodName: String, showResult: String, timestamp: Long) extends LogEvent {
  def show: String                  = s"$methodName: $showResult"
  def paramSeeds: Map[String, Long] = Map.empty
}

object FuncResp {
  implicit val catsEq: Eq[FuncResp] = Eq.instance {
    case (a, b) =>
      a.methodName == b.methodName && a.showResult == b.showResult
  }
}
case class FuncInvocation(methodName: String,
                          paramSeeds: Map[String, Long],
                          showParams: String,
                          showResult: String,
                          start: Long,
                          end: Long)
    extends LogEvent {
  def show: String = s"$methodName($showParams): $showResult"
}

object FuncInvocation {
  implicit val catsEq: Eq[FuncInvocation] = Eq.instance {
    case (a, b) =>
      a.methodName == b.methodName &&
        a.paramSeeds == b.paramSeeds &&
        a.showParams == b.showParams &&
        a.showResult == b.showResult
  }
}
object PrintLogs {
  def pretty(logs: List[List[LogEvent]]): String = {
    val maxLength = logs.flatMap(_.map(_.show.length)).max

    logs.transpose
      .map { row =>
        row
          .map(event => {
            val str = event.show
            s""" $str${new String(Array.fill(maxLength - str.length)(' '))} """
          })
          .mkString("|")
      }
      .mkString("\n")
  }
}
