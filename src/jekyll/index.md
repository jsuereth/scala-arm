---
layout: default
title: Scala Automatic Resource Management
---

Welcome to the scala-arm project!  This project aims to provide Automatic-Resource-Management via a robust library with an easy-to-use interface.  The project aims to support 80% of ARM-related use cases, along with allowing customisation for the remaining cases.  The library uses a lot of advanced features behind the scenes to *do the right thing* for users.  Some of the method signatures may seem strange to users new to scala, so we recommend checking out the basic usage description.

Links:
* [Basic Usage](usage.html)
* [Resource Type Class](resource.html)
* [Mechanics of map and flatMap](flatmap.html)
* [Delimited Continuations and ARM](continuations.html)
* [SocketExample](sockets.html)
* [Scaladoc API](latest/api/index.html)
* Browsable Source

You can find the library on the scala-tools.org maven repository.

    groupId: com.github.jsuereth.scala-arm
    artifactId:  scala-arm_${scala-version}
    version: 1.1
