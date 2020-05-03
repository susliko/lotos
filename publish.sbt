import Publish._

publishVersion := "0.1.0"

ThisBuild / organization := "dev.susliko"
ThisBuild / version := {
  val branch = git.gitCurrentBranch.value
  if (branch == "master") publishVersion.value
  else s"${publishVersion.value}-$branch-SNAPSHOT"
}

ThisBuild / publishMavenStyle := true

ThisBuild / publishTo :=
  (if (!isSnapshot.value) {
     sonatypePublishToBundle.value
   } else {
     Some(Opts.resolver.sonatypeSnapshots)
   })

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/susliko/lotos"),
    "git@github.com:susliko/lotos"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "susliko",
    name = "Vasiliy Morkovkin",
    email = "1istoobig@gmail.com",
    url = url("https://github.com/susliko")
  )
)

ThisBuild / description := "Library for testing concurrent data structures"
ThisBuild / licenses := List("MIT" -> new URL("https://opensource.org/licenses/MIT"))
ThisBuild / homepage := Some(url("https://github.com/susliko/lotos"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ =>
  false
}

ThisBuild / credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credential")
