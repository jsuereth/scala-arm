import sbt._

class ArmProject(info: ProjectInfo) extends DefaultProject(info) {
    val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"

    val jta = "javax.transaction" % "jta" % "1.1" % "provided"

    override def packageDocsJar = defaultJarPath("-javadoc.jar")
    override def packageSrcJar= defaultJarPath("-sources.jar")

}
