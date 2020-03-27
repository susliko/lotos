import Dependencies._
import Publish._

name := "lotos"

version := "0.1"

scalaVersion := "2.13.1"

lazy val setMinorVersion = minorVersion := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) => v.toInt
    case _            => 0
  }
}

lazy val setModuleName = moduleName := {
  s"lotos-${(publishName or name).value}"
}

val macros = Keys.libraryDependencies ++= {
  minorVersion.value match {
    case 13 => Seq(scalaOrganization.value % "scala-reflect" % scalaVersion.value)
    case 12 =>
      Seq(
        compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch),
        scalaOrganization.value % "scala-reflect" % scalaVersion.value
      )
  }
}

lazy val lotosInternal = project
  .in(file("internal"))
  .settings(
    defaultSettings,
    libraryDependencies ++= Seq(shapeless, catsCore, catsEffect)
  )

lazy val lotosMacro = project
  .in(file("macro"))
  .dependsOn(lotosInternal)
  .aggregate(lotosInternal)
  .settings(defaultSettings, macros)

lazy val lotosTesting = project
  .in(file("testing"))
  .dependsOn(lotosInternal, lotosMacro)
  .aggregate(lotosInternal, lotosMacro)
  .settings(defaultSettings)

lazy val examples = project
  .in(file("examples"))
  .dependsOn(lotosTesting)
  .aggregate(lotosTesting)
  .settings(defaultSettings)

lazy val defaultSettings = Seq(
  scalaVersion := "2.13.1",
  setMinorVersion,
  setModuleName,
  defaultScalacOptions,
  libraryDependencies += compilerPlugin("org.typelevel" %% "kind-projector"     % "0.11.0" cross CrossVersion.patch),
  libraryDependencies += compilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
  libraryDependencies ++=
    Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % Version.silencer cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % Version.silencer % Provided cross CrossVersion.full
    ),
  libraryDependencies += scalatest
) ++ scala213Options ++ simulacrumOptions

lazy val scala213Options = Seq(
  scalacOptions ++= {
    minorVersion.value match {
      case 13 => Seq("-Ymacro-annotations")
      case 12 =>
        Seq(
          "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
          "-Ypartial-unification", // Enable partial unification lype constructor inference
          "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
          "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
          "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
          "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
          "-Ywarn-numeric-widen", // Warn when numerics are widened.
          "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.
          "-Xlint:unsound-match", // Pattern match may not be typesafe.
          "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
          "-Xfuture", // Turn on future language features.
          "-Ymacro-debug-verbose",
        )
    }
  }
)

lazy val simulacrumOptions = Seq(
  libraryDependencies ++= Seq(
    scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided,
    simulacrum              % Provided
  ),
  pomPostProcess := { node =>
    import scala.xml.transform.{RewriteRule, RuleTransformer}

    new RuleTransformer(new RewriteRule {
      override def transform(node: xml.Node): Seq[xml.Node] = node match {
        case e: xml.Elem
            if e.label == "dependency" &&
              e.child.exists(child => child.label == "groupId" && child.text == simulacrum.organization) &&
              e.child.exists(child => child.label == "artifactId" && child.text.startsWith(s"${simulacrum.name}_")) =>
          Nil
        case _ => Seq(node)
      }
    }).transform(node).head
  }
)

lazy val defaultScalacOptions = scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-encoding",
  "utf-8", // Specify character encoding used by source files.
  "-explaintypes", // Explain type errors in more detail.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros", // Allow macro definition (besides implementation and application)
  "-language:higherKinds", // Allow higher-kinded types
  "-language:implicitConversions", // Allow definition of implicit functions called views
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-opt:l:method", // Enable intra-method optimizations: unreachable-code,simplify-jumps,compact-locals,copy-propagation,redundant-casts,box-unbox,nullness-tracking,closure-invocations,allow-skip-core-module-init,assume-modules-non-null,allow-skip-class-loading.
  "-opt:l:inline", // Enable cross-method optimizations (note: inlining requires -opt-inline-from): l:method,inline.
  "-opt-inline-from:tofu.**", // Patterns for classfile names from which to allow inlining
  "-opt-warnings:none", // No optimizer warnings.
  "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
  "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
  "-Xlint:option-implicit", // Option.apply used implicit view.
  "-Xlint:package-object-classes", // Class or object defined in package object.
  "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
  "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
  "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals", // Warn if a local definition is unused.
  "-Ywarn-unused:params", // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates", // Warn if a private member is unused.
  "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
  "-Ywarn-extra-implicit" // Warn when more than one implicit parameter section is defined.
)
