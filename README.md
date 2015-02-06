# Scala Automatic Resource Management

[![Join the chat at https://gitter.im/jsuereth/scala-arm](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/jsuereth/scala-arm?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This project is an attempt to provide an Automatic-Resource-Management library for the scala distribution.  It is based off of code contributed to the Scalax project.

## Using scala-arm

In SBT:

    libraryDependencies += "com.jsuereth" %% "scala-arm" % "1.3"

*or*

    libraryDependencies += "com.jsuereth" % "scala-arm_2.9.2" % "1.3"
    libraryDependencies += "com.jsuereth" % "scala-arm_2.10" % "1.3"

In Maven:

    <dependency>
       <groupId>com.jsuereth</groupId>
       <artifactId>scala-arm_${scala.binary.version}</artifactId>
       <version>1.3</version>
    </dependency>


## Examples

Scala-arm provides many ways of managing resources and re-using code.  Here's a few examples.

### Imperative Style
    
    import resource._
    // Copy input into output.
    for(input <- managed(new java.io.FileInputStream("test.txt"));
        output <- managed(new java.io.FileOutputStream("test2.txt"))) {
      val buffer = new Array[Byte](512)
      def read(): Unit = input.read(buffer) match {
        case -1 => ()
        case  n => output.write(buffer,0,size); read()
      }
      read()
    }

### Delimited continuation style

    import resource._
    import java.io.{File, FileInputStream => Fin, FileOutputStream => Fout}
    def copyFile(from: File, to: File): Unit =
      withResources {
        val f = managed(new Fin(from)).reflect[Unit]
        val t = managed(new Fout(to)).reflect[Unit]
        val buffer = new Array[Byte](512)
        def read(): Unit = f.read(buffer) match {
          case -1 => ()
          case  n => t.write(buffer,0,n); read()
        }
        read()
    }

For more information on usage, see [Usage](http://jsuereth.com/scala-arm/usage.html)

## SCALA LICENSE

Copyright (c) 2002-2013 EPFL, Lausanne, unless otherwise specified.
All rights reserved.

This software was developed by the Programming Methods Laboratory of the
Swiss Federal Institute of Technology (EPFL), Lausanne, Switzerland.

Permission to use, copy, modify, and distribute this software in source
or binary form for any purpose with or without fee is hereby granted,
provided that the following conditions are met:

   1. Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

   2. Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.

   3. Neither the name of the EPFL nor the names of its contributors
      may be used to endorse or promote products derived from this
      software without specific prior written permission.


THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.
