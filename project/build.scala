import sbt._
import Keys._
import com.jsuereth.sbtsite.SitePlugin.site
import com.jsuereth.sbtsite.SiteKeys._
import com.jsuereth.ghpages.GhPages.ghpages
import com.jsuereth.git.GitPlugin.git

object PluginDef extends Build {

  val arm = (Project("scala-arm", file(".")) settings(
    organization := "com.github.jsuereth.scala-arm",
    name := "scala-arm",
    version := "1.2-SNAPSHOT",
    scalaVersion := "2.9.1",
    crossScalaVersions := Seq("2.9.1"),
    resolvers += "junit interface repo" at "https://repository.jboss.org/nexus/content/repositories/scala-tools-releases",
    resolvers += "java.net repo" at "http://download.java.net/maven/2/",
    libraryDependencies ++= dependencies,
    autoCompilerPlugins := true,
    addContinuations,
    scalacOptions += "-P:continuations:enable"
  ) settings(publishSettings:_*) settings(websiteSettings:_*))

  def publishSettings: Seq[Setting[_]] = Seq(
    // If we want on maven central, we need to be in maven style.
    publishMavenStyle := true,
    publishArtifact in Test := false,
    // The Nexus repo we're publishing to.
    publishTo <<= version { (v: String) =>
      val nexus = "http://nexus.scala-tools.org/content/repositories/"
      if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "snapshots/") 
      else                             Some("releases"  at nexus + "releases/")
    },
    // Maven central cannot allow other repos.  We're ok here because the artifacts we
    // we use externally are *optional* dependencies.
    pomIncludeRepository := { x => false },
    // Maven central wants some extra metadata to keep things 'clean'.
    pomExtra := (
	    <licenses>
		    <license>
			    <name>BSD-style</name>
			    <url>http://www.opensource.org/licenses/bsd-license.php</url>
			    <distribution>repo</distribution>
		    </license>
	    </licenses>
      <scm>
        <url>git@github.com:jsuereth/scala-arm.git</url>
        <connection>scm:git:git@github.com:jsuereth/scala-arm.git</connection>
      </scm>
      <developers>
        <developer>
          <id>jsuereth</id>
          <name>Josh Suereth</name>
          <url>http://jsuereth.com</url>
        </developer>
      </developers>)
  )

  def websiteSettings: Seq[Setting[_]] = site.settings ++ ghpages.settings ++ Seq(
    git.remoteRepo := "git@github.com:jsuereth/scala-arm.git",
    siteMappings <++= (baseDirectory, target, streams) map { (dir, out, s) => 
      val jekyllSrc = dir / "src" / "jekyll"
      val jekyllOutput = out / "jekyll"
      // Run Jekyll
      sbt.Process(Seq("jekyll", jekyllOutput.getAbsolutePath), Some(jekyllSrc)).!;
      // Figure out what was generated.
      (jekyllOutput ** ("*.html" | "*.png" | "*.js" | "*.css" | "CNAME") x relativeTo(jekyllOutput))
    }
  )

  def addContinuations = libraryDependencies <<= (scalaVersion, libraryDependencies) apply { (v, d) =>
    d :+ compilerPlugin("org.scala-lang.plugins" % "continuations" % v)
  }

  def dependencies = Seq(
    "junit" % "junit" % "4.5" % "test",
    "com.novocode" % "junit-interface" % "0.7" % "test->default",
    "org.apache.derby" % "derby" % "10.5.3.0_1" % "test",
    "javax.transaction" % "jta" % "1.0.1B" % "provided"
  )

  
}
