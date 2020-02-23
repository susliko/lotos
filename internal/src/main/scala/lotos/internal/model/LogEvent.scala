package lotos.internal.model

trait LogEvent {
  def show: String
}

case class FuncCall(method: String, paramSeeds: Map[String, Long], showParams: String) extends LogEvent {
  def show: String = s"$method($showParams)"
}

case class FuncResp(method: String, showResult: String) extends LogEvent {
  def show: String = s"$method: $showResult"
}
case class FuncInvocation(method: String, paramSeeds: Map[String, Long], showParams: String, showResult: String)
    extends LogEvent {
  def show: String = s"$method($showParams): $showResult"
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
