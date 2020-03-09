package lotos.internal.model

trait LogEvent {
  def methodName: String
  def show: String
}

case class FuncCall(methodName: String, paramSeeds: Map[String, Long], showParams: String) extends LogEvent {
  def show: String = s"$methodName($showParams)"
}

case class FuncResp(methodName: String, showResult: String) extends LogEvent {
  def show: String = s"$methodName: $showResult"
}
case class FuncInvocation(methodName: String, paramSeeds: Map[String, Long], showParams: String, showResult: String)
    extends LogEvent {
  def show: String = s"$methodName($showParams): $showResult"
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
