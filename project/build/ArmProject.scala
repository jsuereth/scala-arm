import sbt._

class SampleProject(info: ProjectInfo) extends DefaultProject(info) {
    val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
}
