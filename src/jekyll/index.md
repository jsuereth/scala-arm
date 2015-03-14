---
layout: default
title: Scala Automatic Resource Management
---

Welcome to the scala-arm project!  This project aims to provide Automatic-Resource-Management via a robust library with an easy-to-use interface.  The project aims to support 80% of ARM-related use cases, along with allowing customisation for the remaining cases.  The library uses a lot of advanced features behind the scenes to *do the right thing* for users.  Some of the method signatures may seem strange to users new to scala, so we recommend checking out the basic usage description.

Links:
* [Basic Usage](usage.html)
* [Resource Type Class](resource.html)
* [Mechanics of map and flatMap](flatmap.html)
* [SocketExample](sockets.html)
* [Scaladoc API](latest/api/index.html)
* Browsable Source

You can find the library on the maven central repository.

    groupId: com.jsuereth
    artifactId:  scala-arm_${scala-binary-version}
    version: 2.0


Note: 
* Release can be found on maven central, or for the impatient:  [Sonatype's OSSRH repository](https://oss.sonatype.org/content/groups/public/).
* Snapshots can be found on [Sonatype's OSSRH snapshot repository](https://oss.sonatype.org/content/groups/public).
* In the 2.0 version, all scala continuation based API has been removed, as the continuations plugin is now deprecated.
