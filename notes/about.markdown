# Scala Automatic Resource Management #

scala-arm is an automatic resource management library for Scala.   The scala-arm library provides equivalent and improved fucntionality to Java 7's ARM.

## Basic Usage ##

The Scala ARM library provides three "modes" of operations:

* Imperative style resource management functions.
* A monadic style resource management class.
* A delimited continuation style API.

### Imperative Style ###

The Scala ARM library allows users to ensure opening closing of resources within blocks of code using the <tt>managed</tt> method.   This is easiest to accomplish with a for expression and the managed method defined on <tt>scala.resource</tt>:


    import resource._
    for(input <- managed(new FileInputStream("test.txt")) {
      // Code that uses the input as a FileInputStream
    }


### Monadic Style ###

The scala-arm library defined a monadic like container <tt>ManagedResource</tt>.   This container defines <tt>map</tt> and <tt>flatMap</tt> interfaces.   It can be constructed using the <tt>managed</tt> method defined on <tt>scala.resource</tt>.   The <tt>map</tt> and <tt>flatMap</tt> methods are defined specially, but in generally they will do what you expect them to.   

The <tt>map</tt> method will take a transformation of the raw resource type and return a new managed resource object of the transformed type.   Let's see an example:

    import resource._
    val first_ten_bytes = managed(new FileInputStream("test.txt")) map { 
       input =>
         val buffer = new Array[Byte](10)
         input.read(buffer)
         buffer
    }

This constructs a new resource that will obtain the first ten bytes of a file when aquired, each and every time.  This allows you to build up behavior inside of a ManagedResource monad before attempting to acquire and execute the behavior.   This is an excellent way to construct the means of querying for data and obtaining it fresh when needed.

## Delimtied Continuations ##

The scala-arm library also supports using delimited continuations.   This is done via the <tt>reflect</tt> method on <tt>ManagedResource</tt>.  This can be used to "flatten" the nested blocks required to use resources.   The best example is the <tt>and</tt> method defined on <tt>scala.resource</tt>.   This method can be used to combine two resources into a single <tt>ManagedResource</tt> class containing a tuple of the two resources.   It will jointly open and close both resources.   The code is below:

    import resource._
    def and[A,B](r1 : ManagedResource[A], r2 : ManagedResource[B]) = 
        new ManagedResource[(A,B)] with ManagedResourceOperations[(A,B)] {
          override def acquireFor[C](f : ((A,B)) => C) = withResources {
            f( (r1.reflect[C], r2.reflect[C]) )
          }
        }
