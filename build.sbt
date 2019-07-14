import com.typesafe.tools.mima.core._, ProblemFilters._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.{
  mimaBinaryIssueFilters,
  mimaPreviousArtifacts
}

import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._

import xerial.sbt.Sonatype._

lazy val bcSettings: Seq[Setting[_]] = mimaDefaultSettings ++ Seq(
  mimaPreviousArtifacts := {
    val sv = scalaVersion.value

    if (sv startsWith "2.9.") {
      Set("com.jsuereth" % "scala-arm_2.9.1" % "1.2")
    } else if (sv startsWith "2.10.") {
      Set("com.jsuereth" %% "scala-arm" % "1.2")
    } else if (sv startsWith "2.13.") {
      Set.empty[ModuleID]
    } else {
      Set("com.jsuereth" %% "scala-arm" % "2.0")
    }
  },
  mimaBinaryIssueFilters ++= {
    val missingMethodInOld: ProblemFilter = {
      case ReversedMissingMethodProblem(_) => false
      case DirectMissingMethodProblem(old) => !old.isAccessible
      case InheritedNewAbstractMethodProblem(_, _) => false
      case IncompatibleResultTypeProblem(old, _) => !old.isAccessible
      case IncompatibleMethTypeProblem(old, _) => !old.isAccessible
      case MissingClassProblem(old) => !old.isPublic
      case AbstractClassProblem(old) => !old.isPublic
      case _ => true
    }

    Seq(
      missingMethodInOld, // forward compat
      ProblemFilters.exclude[IncompatibleTemplateDefProblem]("resource.AbstractManagedResource"), // previous change
      ProblemFilters.exclude[MissingTypesProblem]("resource.DefaultManagedResource"), // previous change
      ProblemFilters.exclude[UpdateForwarderBodyProblem]("resource.ManagedResourceOperations.toTraversable") // mixin for 2.13 compat
    )
  }
)

lazy val publishSettings: Seq[Setting[_]] = Seq(
  // If we want on maven central, we need to be in maven style.
  publishMavenStyle := true,
  publishArtifact in Test := false,
  // The Nexus repo we're publishing to.
  publishTo := {
    val nexus = "https://oss.sonatype.org/"

    if (version.value.trim.endsWith("SNAPSHOT")) {
      Some("snapshots" at s"${nexus}content/repositories/snapshots")
    } else {
      Some("releases" at s"${nexus}service/local/staging/deploy/maven2")
    }
  },
  // Maven central cannot allow other repos.  We're ok here because the artifacts we
  // we use externally are *optional* dependencies.
  pomIncludeRepository := { _ => false },
  
  // Maven central wants some extra metadata to keep things 'clean'.
  homepage := Some(url("http://jsuereth.com/scala-arm")),
  licenses += "BSD-Style" -> url(
    "http://www.opensource.org/licenses/bsd-license.php"),
  scmInfo := Some(ScmInfo(url(
    "http://github.com/jsuereth/scala-arm"),
    "scm:git@github.com:jsuereth/scala-arm.git")),
  pomExtra := (
    <developers>
      <developer>
      <id>jsuereth</id>
      <name>Josh Suereth</name>
      <url>http://jsuereth.com</url>
        </developer>
      </developers>),
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(
      action = { state =>
        val extracted = Project extract state

        extracted.runAggregated(
          PgpKeys.publishSigned in Global in extracted.get(
            thisProjectRef), state)
      },
      enableCrossBuild = true
    ),
    releaseStepCommand("sonatypeReleaseAll"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

lazy val websiteSettings: Seq[Setting[_]] = Seq(
  git.remoteRepo := "git@github.com:jsuereth/scala-arm.git",
  includeFilter in Jekyll := (
    "*.html" | "*.png" | "*.js" | "*.css" | "CNAME")
)

val arm = project.in(file(".")).
  enablePlugins(SiteScaladocPlugin).
  settings(Seq(
    organization := "com.jsuereth",
    name := "scala-arm",
    scalaVersion := "2.12.8",
    crossScalaVersions := Seq(
      "2.10.7", "2.11.12", scalaVersion.value, "2.13.0"),
    resolvers += "java.net repo" at "http://download.java.net/maven/2/",
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test,
      "org.apache.derby" % "derby" % "10.15.1.3" % Test,
      "javax.transaction" % "jta" % "1.1" % Provided
    ),
    scalacOptions ++= Seq("-deprecation", "-feature"),
    unmanagedSourceDirectories in Compile += {
      val base = (sourceDirectory in Compile).value

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => base / "scala-2.13+"
        case _                       => base / "scala-2.13-"
      }
    },
    unmanagedSourceDirectories in Test += {
      val base = (sourceDirectory in Test).value

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => base / "scala-2.13+"
        case _                       => base / "scala-2.13-"
      }
    }
  ) ++ sonatypeSettings ++ publishSettings ++ (
    websiteSettings ++ bcSettings))
