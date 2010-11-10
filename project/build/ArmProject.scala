import sbt._

// Helper to use maven credentials file for Ivy publishing
trait MavenCredentialsReader {
  import java.io._

  lazy val userHome = new java.io.File(System.getProperty("user.home"))
  
  def addMavenCredentialsForServer(serverId : String) {
    addMavenCredentialsForServer(serverId,serverId)
  }
  def addMavenCredentialsForServer(serverId : String, name : String)  {
    try {
      val Some((user, password)) = readMavenCredentials(serverId)
      scala.Console.println("Registering credents on " + serverId + " for user " + user)
      Credentials.add(name, serverId, user, password)
    } catch {
      case t => //Ignore errors, jsut log
         scala.Console.println("Unable to load maven credentials for [" + serverId + "]")
    }
  }
  def readMavenCredentials(repoName : String) : Option[(String,String)]  =
    readMavenCredentials(repoName, new java.io.File(userHome, ".m2/settings.xml"))

  /** Reads the given location for maven settings file and attempts to pull the given credentials from the file */
  def readMavenCredentials(repoName : String,
                           fileLocation : java.io.File) : Option[(String,String)] = {

    import xml._
    if(!fileLocation.exists()) {
      System.err.println("Cannot resolve nexus credentials! File not found: " + fileLocation)
      System.exit(1) // Bringing the Hammer of Death!
    }
    val input = new FileInputStream(fileLocation)
    try {
      val xml = XML.load(input)
      val results = for { server <- xml \ "servers" \ "server"
         if ((server \ "id").text) == repoName
      } yield Tuple2((server \ "username").text, (server \ "password").text)
      results.headOption
    } finally {
      input.close() // When will they accept my ARM lib!!!
    }


  }
}



// Our Project File!
class ArmProject(info: ProjectInfo) extends DefaultProject(info) with AutoCompilerPlugins with MavenCredentialsReader {


  // Add snapshot repo for resolving dependencies.
  val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
  //val jta = "javax.transaction" % "jta" % "1.1" % "provided"

  // Compiler plugins we're using
  val cont = compilerPlugin("org.scala-lang.plugins" % "continuations" % buildScalaVersion)
  //val sxr = compilerPlugin("org.scala-tools.sxr" %% "sxr" % "0.2.6")
  override def compileOptions =
          CompileOption("-P:continuations:enable") ::
          //CompileOption("-P:sxr:base-directory:" + mainScalaSourcePath.absolutePath) ::
          Unchecked :: super.compileOptions

  val bryanjswift = "Bryan J Swift Repository" at "http://repos.bryanjswift.com/maven2/"
  val junitInterface = "com.novocode" % "junit-interface" % "0.4.0" % "test"
  val derby = "org.apache.derby" % "derby" % "10.5.3.0_1" % "test"



  // Publishing rules
  override def managedStyle = ManagedStyle.Maven
  val publishTo = "nexus.scala-tools.org" at "http://nexus.scala-tools.org/content/repositories/releases/"
  override def packageDocsJar = defaultJarPath("-javadoc.jar")
  override def packageSrcJar= defaultJarPath("-sources.jar")
  val sourceArtifact = Artifact.sources(artifactID)
  val docsArtifact = Artifact.javadoc(artifactID)
  override def packageToPublishActions = super.packageToPublishActions ++ Seq(packageDocs, packageSrc)
  override def pomExtra =
    <licenses>
      <license>
        <name>Scala License</name>
        <url>http://www.scala-lang.org/node/146</url>
        <distribution>repo</distribution>
      </license>
    </licenses>

  
  addMavenCredentialsForServer("nexus.scala-tools.org", "Sonatype Nexus Repository Manager")

}
