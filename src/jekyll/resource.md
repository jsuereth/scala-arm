---
layout: default
title: Resource Typeclass
---
The scala-arm library uses a Resource type class trait, conveniently called `Resource` to control how resources are opened/closed within ARM blocks.   The basic trait is shown below:


{% highlight scala %}
package scala.resource
trait Resource[R] {
  def open(r : R) : Unit = ()
  def close(r : R) : Unit
  def possibleExceptions : Seq[Class[_]] = List(classOf[Exception])
}
{% endhighlight %}

The trait defines three methods, `open`, `close` and `possibleExceptions`.  The `open` method only needs to be defined if the resource needs to be explicitly opened.   The `possibleExceptions` method defines what exceptions can be caught and defferred while attempting to ensure the resource is closed.   If an exception needs to be fatal, i.e. prevent the resource from attempting to close, then it should *not* be included in this list.

Type classes in scala are encoded using implicit parameters.   Due to the mechanisms of implicit lookup, the scala-arm library is able to provide default implicits that will be used if no user-defined implicit is available for a given type.   To be flexible, scala-arm provides the following two implicits at the bottom of the lookup hierarchy, i.e. they will be the last ones Scala attempts to use before rejected a call to `managed`:

{% highlight scala %}
  type ReflectiveCloseable = { def close() }
  implicit def reflectiveCloseableResource[A <: ReflectiveCloseable] = new Resource[A] {
    override def close(r : A) = r.close()
    override def toString = "Resource[{ def close() : Unit }]"
  }
  type ReflectiveDisposable = { def dispose() }
  implicit def reflectiveDisposableResource[A <: ReflectiveDisposable] = new Resource[A] {
    override def close(r : A) = r.dispose()
    override def toString = "Resource[{ def dispose() : Unit }]"
  }
{% endhighlight %}

These two implicits allow users of the library to make use of any resource class that defines a close or dispose method.  The downside is that by using structural types, the close and dispose method will be invoked via reflection.

The scala-arm library also provides default implicits for common resources used from the Scala library or the Java SDK.  In particular, anything that extends `java.io.Closeable` will *not* use reflection when closing the resource.  This type class is given slightly higher priority so it will be preferred to the reflective version when resolving implicits.
