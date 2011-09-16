// The plugin is only published for 2.8.1, 2.9.0-1 and 2.9.1
libraryDependencies <<= (scalaVersion, libraryDependencies) { (scv, deps) =>
    if ((scv == "2.8.1") || (scv == "2.9.0-1") || (scv == "2.9.1"))
        deps :+ ("org.clapper" %% "sbt-lwm" % "0.1.4")
    else
        deps
}
