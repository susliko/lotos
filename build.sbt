ThisBuild / name := "lotos"
ThisBuild / scalaVersion := "2.13.3"

lazy val scalaVersions = List("2.12.12", "2.13.3")

lazy val commonDependencies =
  libraryDependencies ++= List(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.typelevel"  %% "cats-core"    % "2.2.0",
    "org.typelevel"  %% "cats-effect"  % "2.2.0",
    "com.chuusai"    %% "shapeless"    % "2.3.3",
    "org.scalatest"  %% "scalatest"    % "3.2.2" % Test,
  )

def configure(id: String)(project: Project): Project =
  project.settings(
    moduleName := s"lotos-$id",
    crossScalaVersions := scalaVersions,
    sources in (Compile, doc) := List.empty,
    commonDependencies,
    scalacOptions ++= List(
      "-language:experimental.macros",
    ),
    libraryDependencies += compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 13)) => Nil
        case _             => List(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch))
      }
    },
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 13)) => List("-Ymacro-annotations")
        case _             => Nil
      }
    }
  )

def lotosModule(id: String) =
  Project(id, file(s"$id"))
    .configure(configure(id))

lazy val lotosModel = lotosModule("model")
lazy val lotosInternal = lotosModule("internal")
  .dependsOn(lotosModel)
  .dependsOn(lotosModel)
lazy val lotosMacros = lotosModule("macros")
  .dependsOn(lotosModel, lotosInternal)
  .aggregate(lotosModel, lotosInternal)
lazy val lotosTesting = lotosModule("testing")
  .dependsOn(lotosModel, lotosInternal, lotosMacros)
  .aggregate(lotosModel, lotosInternal, lotosMacros)
lazy val lotosExamples = lotosModule("examples")
  .settings(
    skip in publish := true
  )
  .dependsOn(lotosTesting)
  .aggregate(lotosTesting)

lazy val modules: List[ProjectReference] = List(lotosModel, lotosInternal, lotosMacros, lotosTesting, lotosExamples)

lazy val lotos = project
  .in(file("."))
  .settings(
    moduleName := "lotos",
    skip in publish := true
  )
  .aggregate(modules: _*)
