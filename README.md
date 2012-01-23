# Scala Automatic Resource Management

This project is an attempt to provide an Automatic-Resource-Management library for the scala distribution.  It is based off of code contributed to the Scalax project.

## Using scala-arm

In SBT:

    libraryDependencies += "com.jsuereth" %% "scala-arm" % "1.2"

In Maven:

    <dependency>
       <groupId>com.jsuereth</groupId>
       <artifactId>scala-arm_${scala.version}</artifactId>
       <version>1.2</version>
    </dependency>


## Basic Usage

The Scala ARM library provides three "modes" of operations:
  * Imperative style resource management functions.
  * A monadic style resource management class.
  * A delimited continuation style API.

### Imperative Style

The Scala ARM library allows users to ensure opening closing of resources within blocks of code using the <tt>managed</tt> method.   This is easiest to accomplish with a for expression and the managed method defined on <tt>scala.resource</tt>:

    import resource._
    for(input <- managed(new FileInputStream("test.txt")) {
      // Code that uses the input as a FileInputStream
    }


The <tt>managed</tt> method essentially takes an argument of "anything that has a close or dispose method" and constructs a new <tt>ManagedResource</tt> object.   This object has a <tt>foreach</tt> method which can be used inside of the for expression.  The scala-arm library provides a very flexible mechanism for customising the treatment of resource types, using a type class trait.   Please read the section on the [[Resource Type Class]] for more information.

This style of usage will ensure that the file input stream is closed at the end of the for expression.   In the event of an exception, the originating exception (from inside the for block) will be thrown and any exceptions thrown while closing the resource will be suppressed.   The benefits of using for expressions is that multiple resources can be managed together.  For example, one can do the following:

    import resource._
    // Copy input into output.
    for(input <- managed(new java.io.FileInputStream("test.txt"); 
        output <- managed(new java.io.FileOutputStream("test2.txt")) {
      val buffer = new Array[Byte](512)
      while(input.read(buffer) != -1) {
        output.write(buffer);
      }
    }


There is a convenience notation for those who don't like using for comprehensions:

    import resource._
    managed(DriverManager.getConnection(url, username, password)) acquireAndGet {
      connection =>
        // Something that uses connection
    }


### Monadic style

The scala-arm library defined a monadic like container <tt>ManagedResource</tt>.   This container defines <tt>map</tt> and <tt>flatMap</tt> interfaces.   It can be constructed using the <tt>managed</tt> method defined on <tt>scala.resource</tt>.   The <tt>map</tt> and <tt>flatMap</tt> methods are defined specially, but in generally they will do what you expect them to.   

The <tt>map</tt> method will take a transformation of the raw resource type and return a new managed resource object of the transformed type.   Let's see an example:

    import resource._
    val first_ten_bytes = managed(new FileInputStream("test.txt")) map { 
       input =>
         val buffer = new Array[Byte](10)
         input.read(buffer)
         buffer
    }

The <tt>ManagedResource</tt> class also defines mechanisms for extracting data outside of the monadic container after the container has been mapped or flatMapped.  This is done through the opt or either methods.   Both methods attempt to acquire the resource and run all transformations on the resource.  They then close the resource and return a result.   In the case of an error, the opt method will return an empty option.  The either method will return an Either where the right side is defined and left contains the exceptions seen during the execution of the transformations or closing the resource.

    scala> first_ten_bytes.opt.get
    res1: Array[Byte] = Array(72, 65, 73, 32, 10, 85, 10, 87, 85, 82)

    first_ten_bytes.either.right.get
    res2: Array[Byte] = Array(72, 65, 73, 32, 10, 85, 10, 87, 85, 82)</code></pre>

The <tt>flatMap</tt> method can be used to ensure that applying a transformation of an embedded resource to another <tt>ManagedResource</tt> will create a <tt>ManagedResource[T]</tt> instead of a <tt>ManagedResource[ManagedResource[T]]</tt>.

The <tt>ManagedResource</tt> class also supports a <tt>toTraversable</tt> method.  Let's look at an example that will print all lines in the file "test.txt":

    import scala.resource._
    import java.io._
    val reader: ManagedResource[BufferedReader] = managed(new FileInputStream("test.txt")) map (new BufferedReader(new InputStreamReader(_))) 
    val lines: ManagedTraversable[String] = reader map makeBufferedReaderLineIterator toTraversable
    lines.view map (_.trim) foreach println

Much of the noise in the example is dealing with the java.io API.   The important piece is how we have a managed resource, convert it into a traversable and make some minor modification before printing.   This produces a traversable that will eventually read the file.   This allows us to pre-construct I/O related portions of our program to re-use over and over.   For example,  One could construct a <tt>ManagedResource</tt> that will read and parse configuration information.   Then you can use a listener that detects when the file's modification date changes, and re-extract the configuration information from the <tt>ManagedResource</tt>.

The <tt>ManagedTraversable</tt> collection type is strict.   If you wish to do several map/filter/etc. operations before aquiring the resource and traversing the collection, you should immediately change to a <tt>view</tt> as shown in the example.

### Delimited continuation style

The scala-arm library also supports using delimited continuations.   This is done via the <tt>reflect</tt> method on <tt>ManagedResource</tt>.  This can be used to "flatten" the nested blocks required to use resources.   The best example is the <tt>and</tt> method defined on <tt>scala.resource</tt>.   This method can be used to combine two resources into a single <tt>ManagedResource</tt> class containing a tuple of the two resources.   It will jointly open and close both resources.   The code is below:

    import resource._
    def and[A,B](r1 : ManagedResource[A], r2 : ManagedResource[B]) = 
        new ManagedResource[(A,B)] with ManagedResourceOperations[(A,B)] {
          override def acquireFor[C](f : ((A,B)) => C) = withResources {
            f( (r1.reflect[C], r2.reflect[C]) )
          }
        }

compare that with the previous "imperative style" <tt>and</tt> method:

    def and[A,B](r1 : ManagedResource[A], r2 : ManagedResource[B]) : ManagedResource[(A,B)] = 
      new ManagedResource[(A,B)] with ManagedResourceOperations[(A,B)] {
        override def acquireFor[C](f : ((A,B)) => C) : Either[List[Throwable], C] = {
          val result = r1.acquireFor({ opened1 =>
            r2.acquireFor({ opened2 =>
              f((opened1, opened2))
            })
          })
          result.fold( errors => Left(errors), y => y)      
        }
      }
    }

The mechanism for using Delimted Continuations is outlines in more detail on the [Delimited Continuations and ARM](http://github.com/jsuereth/scala-arm/wiki/Delimited-Continuations-and-ARM) page.

## SCALA LICENSE

Copyright (c) 2002-2009 EPFL, Lausanne, unless otherwise specified.
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
