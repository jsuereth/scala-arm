import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object Compiler extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin

  override def projectSettings = Seq(
    scalacOptions in Compile ++= Seq(
      "-encoding", "UTF-8",
      "-target:jvm-1.8",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xfatal-warnings",
      "-Xlint",
      "-g:vars"
    ),
    scalacOptions in Compile ++= {
      if ((scalaVersion in Compile).value startsWith "2.12.") {
        Seq(
          "-Ywarn-numeric-widen",
          "-Ywarn-infer-any",
          "-Ywarn-dead-code",
          "-Ywarn-unused",
          "-Ywarn-unused-import",
          "-Ywarn-value-discard")
      } else {
        Seq.empty[String]
      }
    },
    scalacOptions in (Compile, console) ~= {
      _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
    },
    scalacOptions in (Test, console) ~= {
      _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
    },
    scalacOptions in Test ++= Seq("-Yrangepos"),
    scalacOptions ~= (_.filterNot(_ == "-Xfatal-warnings")),
    libraryDependencies ++= {
      if (scalaVersion.value startsWith "2.10.") {
        Seq.empty[ModuleID]
      } else {
        val ver = "1.4.1"

        Seq(
          compilerPlugin(
            "com.github.ghik" %% "silencer-plugin" % ver),
          "com.github.ghik" %% "silencer-lib" % ver % Provided)
      }
    }
  )
}
