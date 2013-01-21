---
layout: default
title: Basic Usage
---

The Scala ARM library provides three "modes" of operations:

* Imperative style resource management functions.
* A monadic style resource management class.
* A delimited continuation style API.

## Imperative Style ##

The Scala ARM library allows users to ensure opening closing of resources within blocks of code using the `managed` method.   This is easiest to accomplish with a for expression and the managed method defined on <tt>scala.resource</tt>:

{% highlight scala %}
import resource._
for(input <- managed(new FileInputStream("test.txt")) {
  // Code that uses the input as a FileInputStream
}
{% endhighlight %}


The `managed` method essentially takes an argument of "anything that has a close or dispose method" and constructs a new `ManagedResource` object.   This object has a `foreach` method which can be used inside of the for expression.  The scala-arm library provides a very flexible mechanism for customising the treatment of resource types, using a type class trait.   Please read the section on the [Resource Type Class](resource.html) for more information.

This style of usage will ensure that the file input stream is closed at the end of the for expression.   In the event of an exception, the originating exception (from inside the for block) will be thrown and any exceptions thrown while closing the resource will be suppressed.   The benefits of using for expressions is that multiple resources can be managed together.  For example, one can do the following:

{% highlight scala %}
import resource._
// Copy input into output.
for(input <- managed(new java.io.FileInputStream("test.txt"); 
     output <- managed(new java.io.FileOutputStream("test2.txt")) {
  val buffer = new Array[Byte](512)
  def read(): Unit = input.read(buffer) match {
    case -1 => ()
    case  n => output.write(buffer,0,n); read()
  }
}
{% endhighlight %}


There is a convenience notation for those who don't like using for comprehensions:

{% highlight scala %}
import resource._
managed(DriverManager.getConnection(url, username, password)) acquireAndGet {
  connection =>
   // Something that uses connection
}
{% endhighlight %}

## Monadic style ##

The scala-arm library defined a monadic like container `ManagedResource`.   This container defines `map` and `flatMap` interfaces.   It can be constructed using the `managed` method defined on `scala.resource`.   The `map` and `flatMap` methods are defined to allow monadic workflows.

The `map` method will take a transformation of the raw resource type and return a new managed resource object of the transformed type.   Let's see an example:

{% highlight scala %}
import resource._
val first_ten_bytes = managed(new FileInputStream("test.txt")) map { 
  input =>
     val buffer = new Array[Byte](10)
     input.read(buffer)
     buffer
}
{% endhighlight %}

The `ManagedResource` class also defines mechanisms for extracting data outside of the monadic container after the container has been mapped or flatMapped.  This is done through the opt or either methods.   Both methods attempt to acquire the resource and run all transformations on the resource.  They then close the resource and return a result.   In the case of an error, the opt method will return an empty option.  The either method will return an Either where the right side is defined and left contains the exceptions seen during the execution of the transformations or closing the resource.

    scala> first_ten_bytes.opt.get
    res1: Array[Byte] = Array(72, 65, 73, 32, 10, 85, 10, 87, 85, 82)
    
    scala> first_ten_bytes.either.right.get
    res2: Array[Byte] = Array(72, 65, 73, 32, 10, 85, 10, 87, 85, 82)</code></pre>

The `flatMap` method can be used to ensure that applying a transformation of an embedded resource to another `ManagedResource` will create a `ManagedResource[T]` instead of a `ManagedResource[ManagedResource[T]]`.

The handy mechanism of `ManagedResource` is the ability to create a collection out of a `ManagedResource[Traversable[T]]`.  This can be used to construct a workflow that will open a resource, iterate over its contents, and close it when finished.  Let's look at an example that will print all lines in the file "test.txt":

{% highlight scala %}
import scala.resource._
import java.io._
val stream = managed(new FileInputStream("text.txt"))
val reader = stream map (new BufferedReader(new InputStreamReader(_)))
val lines = stream map makeBufferedReaderLineIterator toTraversable
lines.view map (_.trim) foreach println
{% endhighlight %}

Much of the noise in the example is dealing with the java.io API.   The important piece is how we have a managed resource, convert it into a traversable and make some minor modification before aquiring.   This produces a traversable that will eventually read the file.   This allows us to pre-construct I/O related portions of our program to re-use over and over.   For example,  One could construct a `ManagedResource` that will read and parse configuration information.   Then you can use a listener that detects when the file's modification date changes, and re-extract the configuration information from the `ManagedResource`.

## Delimited continuation style ##

The scala-arm library also supports using delimited continuations.   This is done via the `reflect` method on `ManagedResource`.  This can be used to "flatten" the nested blocks required to use resources.   The best example is the `and` method defined on `scala.resource`.   This method can be used to combine two resources into a single `ManagedResource` class containing a tuple of the two resources.   It will jointly open and close both resources.   The code is below:

{% highlight scala %}
import resource._
def and[A,B](r1 : ManagedResource[A], r2 : ManagedResource[B]) = 
    new ManagedResource[(A,B)] with ManagedResourceOperations[(A,B)] {
      override def acquireFor[C](f : ((A,B)) => C) = withResources {
        f( (r1.reflect[C], r2.reflect[C]) )
      }
  }
{% endhighlight %}

compare that with the previous "imperative style" `and` method:

{% highlight scala %}
def and[A,B](r1 : ManagedResource[A], r2 : ManagedResource[B]) : ManagedResource[(A,B)] = 
  new ManagedResource[(A,B)] with ManagedResourceOperations[(A,B)] {
    override def acquireFor[C](f : ((A,B)) => C) : Either[List[Throwable], C] = {
      val result = r1 acquireFor { opened1 =>
        r2 acquireFor { opened2 =>
          f((opened1, opened2))
        }
       }
      result.fold( errors => Left(errors), y => y)
      }
    }
}
{% endhighlight %}

The mechanism for using Delimited Continuations is outlines in more detail on the [Delimited Continuations and ARM](continuations.html) page.
