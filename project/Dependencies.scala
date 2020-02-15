import sbt._
import Keys._

object Dependencies {
  val minorVersion = SettingKey[Int]("minor scala version")

  object Version {
    val simulacrum = "1.0.0"
    val silencer = "1.4.4"
    val sttp = "2.0.0-RC9"
    val shapeless = "2.3.3"
    val cats = "2.0.0"
  }

  val catsCore = "org.typelevel" %% "cats-core" % Version.cats
  val shapeless = "com.chuusai" %% "shapeless" % Version.shapeless
  val simulacrum = "org.typelevel" %% "simulacrum" % Version.simulacrum
  val sttp = "com.softwaremill.sttp.client" %% "core" % Version.sttp

}
