// -----------------------------------------------------------------------------
//
//  scala.arm - The Scala Incubator Project
//  Copyright (c) 2009 The Scala Incubator Project. All rights reserved.
//
//  The primary distribution site is http://jsuereth.github.com/scala-arm
//
//  This software is released under the terms of the Revised BSD License.
//  There is NO WARRANTY.  See the file LICENSE for the full text.
//
// -----------------------------------------------------------------------------


package resource

import _root_.scala.collection.Seq
import _root_.scala.util.control.Exception


/**
 * An implementation of an ExtractableManagedResource that defers all processing until the user pulls out information using
 * either or opt functions.
 */
private[resource] class DeferredExtractableManagedResource[+A,R](val resource : ManagedResource[R], val translate : R => A) extends 
  ExtractableManagedResource[A] with ManagedResourceOperations[A] { self =>

  override def acquireFor[B](f : A => B) : Either[List[Throwable], B] = resource acquireFor translate.andThen(f)

  override def either = resource acquireFor translate

  override def opt = either.right.toOption

  override def equals(that : Any) = that match {
    case x : DeferredExtractableManagedResource[A,R] => (x.resource == resource) && (x.translate == translate)
    case _ => false
  }
  override def hashCode() : Int = (resource.hashCode << 7) + translate.hashCode + 13

  override def toString = "DeferredExtractableManagedResource(" + resource + ", " + translate + ")"
}


/**
 * Abstract class implementing most of the managed resource features in terms of an open and close method.   This
 * is a refinement over ManagedResourceOperations as it defines the acquireForMethod generically using the
 * scala.util.control.Exception API.
 */
trait AbstractManagedResource[R] extends ManagedResource[R] with ManagedResourceOperations[R] {

  /** 
   * Opens a given resource, returning a handle to execute against during the "session" of the resource being open.
   */
  protected def open : R

  /**
   * Closes a resource using the handle.  This method will throw any exceptions normally occuring during the close of
   * a resource.
   */
  protected def unsafeClose(handle : R) : Unit

  /**
   * The list of exceptions that get caught during ARM and will always be rethrown (considered 'fatal')
   */
  protected def rethrownExceptions : Seq[Class[_]] = List(classOf[java.lang.VirtualMachineError],
                                                          classOf[java.lang.InterruptedException],
                                                          classOf[scala.util.control.ControlThrowable])
  /** Throws an exception if it is the rethrow list. */
  private def rethrowIfBad(t : Throwable)  : Throwable =
    if (rethrownExceptions.exists(_.isInstance(t))) {
      throw t
    } else t

  override def acquireFor[B](f : R => B) : Either[List[Throwable], B] = {
    import Exception._
    val handle = open
    val result  = catching(classOf[java.lang.Throwable]) either (f(handle))
    val close = catching(classOf[java.lang.Throwable]) either unsafeClose(handle)
    // Combine resulting exceptions as necessary.   Finally, throw any exceptions
    // That we can't hold onto (like ControlThrowable).
    result.left.map[List[Throwable]]( _ :: close.left.toOption.toList).left.map {
      exceptions => exceptions.map(rethrowIfBad)
    }
  }
}

/**
 * This is the default implementation of a ManagedResource that makes use of the Resource type trait.
 */
final class DefaultManagedResource[R : Resource : Manifest](r : => R) extends AbstractManagedResource[R] { self =>
  /** Stable reference to the Resource type trait.*/
  protected val typeTrait = implicitly[Resource[R]]
  /**
   * Opens a given resource, returning a handle to execute against during the "session" of the resource being open.
   */
  override protected def open : R = {
    val resource = r
    typeTrait.open(resource)
    resource
  }

  /**
   * Closes a resource using the handle.  This method will throw any exceptions normally occurring during the close of
   * a resource.
   */
  override protected def unsafeClose(r : R) : Unit = typeTrait.close(r)

  /**
   * The list of exceptions that get caught during ARM and will not prevent a call to close.
   */
  override protected def rethrownExceptions : Seq[Class[_]] = typeTrait.fatalExceptions
  /* You cannot serialize resource and send them, so referential equality should be sufficient. */
  /** Add the type trait to help disperse resources */
  override def hashCode() : Int = (typeTrait.hashCode << 7) + super.hashCode + 13
  // That's right, we use manifest solely for nicer toStrings!
  override def toString = "Default[" + implicitly[Manifest[R]] + " : " + typeTrait + "](...)"
}
