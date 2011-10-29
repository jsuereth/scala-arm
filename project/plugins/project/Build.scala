import sbt._
object PluginDef extends Build {
  override def projects = Seq(root)
  lazy val root = Project("plugins", file(".")) dependsOn(ghpages)
  lazy val ghpages = uri("git://github.com/jsuereth/xsbt-ghpages-plugin.git")
  //lazy val lwm = uri("git://github.com/bmc/sbt-lwm.git#release-0.2.1")
  //lazy val posterous = uri("git://github.com/n8han/posterous-sbt.git#0.3.2")
  //lazy val helix = ProjectRef(file("/home/jsuereth/projects/personal/helix/"), "sbt-helix-plugin")
}
