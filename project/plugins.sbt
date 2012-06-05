resolvers += Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.6.1")

addSbtPlugin("com.jsuereth" % "sbt-ghpages-plugin" % "0.4.0")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.3")
