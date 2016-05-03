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
import _root_.scala.util.control.ControlThrowable

/**
 * An implementation of an ExtractableManagedResource that defers all processing until the user pulls out information using
 * either or opt functions.
 */
private[resource] class DeferredExtractableManagedResource[+A,R](val resource: ManagedResource[R], val translate: R => A)
  extends ExtractableManagedResource[A] with ManagedResourceOperations[A] { self =>

  override def acquireFor[B](f: A => B) : ExtractedEither[List[Throwable], B] = resource acquireFor (translate andThen f)

  override def either = ExtractedEither(resource acquireFor translate)

  override def opt = either.either.right.toOption

  override def tried = scala.util.Try(resource apply translate)

  override def equals(that: Any) = that match {
    case x : DeferredExtractableManagedResource[A,R] => (x.resource == resource) && (x.translate == translate)
    case _ => false
  }
  override def hashCode(): Int = (resource.hashCode << 7) + translate.hashCode + 13

  override def toString = "DeferredExtractableManagedResource(" + resource + ", " + translate + ")"
}


/**
 * Abstract class implementing most of the managed resource features in terms of an open and close method.   This
 * is a refinement over ManagedResourceOperations as it defines the acquireForMethod generically using the
 * scala.util.control.Exception API.
 */
abstract class AbstractManagedResource[R] extends ManagedResource[R] with ManagedResourceOperations[R] {

  /** 
   * Opens a given resource, returning a handle to execute against during the "session" of the resource being open.
   */
  protected def open: R

  /**
   * Closes a resource using the handle. This method will throw any exceptions normally occurring during the closing of
   * a resource.
   */
  protected def unsafeClose(handle: R, errors: Option[Throwable]): Unit

  /** These are a list of exceptions we *have* to rethrow, regardless of
   *  a users desires to ensure that thread/return behavior in scala is accurate.
   */
  protected def isRethrown(t: Throwable): Boolean = t match {
    case _: ControlThrowable      => true
    case _: InterruptedException  => true
    case _                        => false    
  }
  
  /** This checks to see if an exception should not be caught, under any circumstances.
   * These usually denote fatal program flaws.
   */
  protected def isFatal(t: Throwable): Boolean = t match {
    case _: java.lang.VirtualMachineError => true
    // TODO - Others?
    case _                                => false
  }
  /** A catcher of exceptions that will ignore those we consider fatal. */
  private final val catchingNonFatal: Exception.Catch[Nothing] = 
    (new Exception.Catch(Exception.mkThrowableCatcher(e => !isFatal(e), throw _), None, _ => false) 
     withDesc "<non-fatal>")

  override def acquireFor[B](f : R => B) : ExtractedEither[List[Throwable], B] = {
    import Exception._
    val handle = open
    val result = catchingNonFatal either (f(handle))
    val close  = catchingNonFatal either unsafeClose(handle, result.left.toOption)
    // Here we pattern match to make sure we get all the errors.
    val either = (result, close) match {
      case (Left(t1), _       ) if isRethrown(t1)       => throw t1
      case (Left(t1), Left(t2))                         => Left(t1 :: t2 :: Nil)
      case (Left(t1), _       )                         => Left(t1 :: Nil)
      case (Right(ExtractedEither(Left(ts))), Left(t2)) => Left(ts.asInstanceOf[List[Throwable]] :+ t2)
      case (_,        Left(t2))                         => Left(t2 :: Nil)
      case (Right(r), _       )                         => Right(r)
    }
    ExtractedEither(either)
  }
}

/**
 * This is the default implementation of a ManagedResource that makes use of the Resource type trait.
 */
final class DefaultManagedResource[R : Resource : Manifest](r : => R) extends AbstractManagedResource[R] { self =>
  /** Stable reference to the Resource type trait.*/
  protected val typeTrait = implicitly[Resource[R]]
  override protected def open: R = {
    val resource = r
    typeTrait.open(resource)
    resource
  }
  override protected def unsafeClose(r: R, error: Option[Throwable]): Unit =
    error match {
      case None    => typeTrait.close(r)
      case Some(t) => typeTrait.closeAfterException(r, t)
    }
  override protected def isFatal(t: Throwable): Boolean =
    typeTrait isFatalException t
  override protected def isRethrown(t: Throwable): Boolean =
    typeTrait isRethrownException t
  /* You cannot serialize resource and send them, so referential equality should be sufficient. */
  override def hashCode(): Int = (typeTrait.hashCode << 7) + super.hashCode + 13
  // That's right, we use manifest solely for nicer toStrings!
  override def toString = "Default[" + implicitly[Manifest[R]] + " : " + typeTrait + "](...)"
}

/**
  * ConstantManagedResource encapsulates a constant value with no associated resource management.
  */
final class ConstantManagedResource[V](value: V) extends AbstractManagedResource[V] {

  /**
   * Simply return the value given at construction.
   */
  override protected def open: V = {
    value
  }

  /**
   * Nothing needs to be done to close.
   */
  override protected def unsafeClose(handle: V, errors: Option[Throwable]): Unit = {}
}
