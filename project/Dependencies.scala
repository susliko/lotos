import sbt._
import Keys._

object Dependencies {
  val minorVersion = SettingKey[Int]("minor scala version")

  object Version {
    val simulacrum = "1.0.0"
    val silencer = "1.4.4"
  }

  val simulacrum = "org.typelevel" %% "simulacrum" % Version.simulacrum

}