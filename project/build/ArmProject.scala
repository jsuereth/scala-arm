import sbt._

class ArmProject(info: ProjectInfo) extends DefaultProject(info) with AutoCompilerPlugins {
  val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
  val jta = "javax.transaction" % "jta" % "1.1" % "provided"


  override def packageDocsJar = defaultJarPath("-javadoc.jar")
  override def packageSrcJar= defaultJarPath("-sources.jar")

  val cont = compilerPlugin("org.scala-lang.plugins" % "continuations" % "2.8.0-SNAPSHOT")
  override def compileOptions = CompileOption("-P:continuations:enable") :: super.compileOptions


  val bryanjswift = "Bryan J Swift Repository" at "http://repos.bryanjswift.com/maven2/"
  val junitInterface = "com.novocode" % "junit-interface" % "0.4.0" % "test"
  val derby = "org.apache.derby" % "derby" % "10.5.3.0_1" % "test"

}
