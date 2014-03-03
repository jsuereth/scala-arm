resolvers += Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

// Note:  Add this to ~/.sbt/plugins/gpg.sbt if you want to sign artifacts.
//addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.2")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.6")
