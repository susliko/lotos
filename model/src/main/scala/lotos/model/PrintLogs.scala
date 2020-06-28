package lotos.model

import java.nio.file.{Files, Paths}

import cats.effect.{IO, Sync}

object PrintLogs {
  case class LogsWithTimes(logs: Vector[Vector[String]], maxLogLength: Int)

  private def logsWithTimes(logs: Vector[Vector[TestLog]], scenarioLength: Int): LogsWithTimes = {
    val sortedTimes = logs
      .flatMap(_.flatMap(event => List(event.call.timestamp, event.resp.timestamp)))
      .sorted
      .zipWithIndex

    val maxTime = sortedTimes.length

    val timesMapping =
      sortedTimes.toMap
        .mapValues(_.toLong)

    val maxLogLength = logs.flatMap(_.map(_.show(maxTime, maxTime).length)).max

    LogsWithTimes(
      logs = logs
        .map { column =>
          column
            .map(event => {
              val str =
                event.show(timesMapping(event.call.timestamp), timesMapping(event.resp.timestamp))
              s""" $str${" " * (maxLogLength - str.length)} """
            })
        },
      maxLogLength = maxLogLength
    )
  }

  def prettyString(rawLogs: Vector[Vector[TestLog]], scenarioLength: Int): String = {
    val LogsWithTimes(logs, maxLogLength) = logsWithTimes(rawLogs, scenarioLength)
    val emptyLog                          = " " * (maxLogLength + 2)
    logs
      .fold(Vector.fill(scenarioLength)("")) {
        case (l1, l2) => l1.zipAll(l2, emptyLog, emptyLog).map { case (s1, s2) => s"$s1|$s2" }
      }
      .mkString("\n")
  }

  def diagram[F[_]](rawLogs: Vector[Vector[TestLog]], scenarioLength: Int, fileName: String)(
      implicit F: Sync[F]
  ): F[Unit] = {
    val LogsWithTimes(logs, maxLogLength) = logsWithTimes(rawLogs, scenarioLength)
    val diagramContent                    = htmlDiagram()
    F.delay(Files.write(Paths.get(fileName), diagramContent.getBytes()))
  }

  private def htmlDiagram(): String =
    """|<!DOCTYPE html>
      |<html>
      |    <head>
      |        <style> 
      |            body{
      |                width: 100%;
      |                height: 100%;
      |                padding: 0;
      |                margin: 0;
      |            }
      |            
      |            .rectangle{
      |                border: 1px solid black;
      |                border-radius: 10px;
      |                height: 45px;   
      |                text-align: center; 
      |                overflow: scroll;
      |                padding: 0 5px;
      |            }
      |
      |            .line{
      |                display: block;
      |                box-sizing: border-box;
      |                margin-top: 22.5px;
      |                height: 0;
      |                border: 0.75px solid black;
      |            }
      |
      |            .arrow {
      |                align-self: stretch;    
      |                margin-top: 16px;
      |                min-width: 15px;
      |                height: 15px;
      |                border-top: 1.2px solid #000;
      |                border-right: 1.2px solid #000;
      |                transform: rotate(45deg);
      |                -webkit-transform: rotate(45deg);
      |            }
      |
      |            .process{
      |                position: relative;
      |                margin: 40px 10px;
      |                display: grid;
      |                min-width: 80%;
      |                grid-template-rows: auto;
      |                grid-auto-flow: row;
      |
      |            }
      |
      |            .processes{
      |                position: absolute;
      |                min-width: 70vw;
      |                overflow: scroll;
      |            }
      |    </style>
      |    
      |    </head>
      |    <body>
      |        <div class='processes'>
      |        <div class='process' style='grid-template-columns: 5vw 15vw 10vw 5vw 1fr 16px;'>
      |            <div class='line'></div>
      |            <div class='rectangle'> <p>Info about other step and more information and more and more and more and i aint gonna stop</p></div>
      |            <div class='line'></div>
      |            <span class='arrow'></span>
      |        </div>
      |        <div class='process' style='grid-template-columns: 10vw 5vw 10vw 15vw 1fr 16px;'>
      |            <div class='line'></div>
      |            <div class='rectangle'> <p>Information about step </p></div>
      |            <div class='line'></div>
      |            <div class='rectangle'> <p>Info about other step and more information and more and more and more and i aint gonna stop</p></div>
      |            <div class='line'></div>    
      |            <span class='arrow'></span>
      |        </div>
      |    </div>
      |    </body>
      |</html>
      |""".stripMargin
}
