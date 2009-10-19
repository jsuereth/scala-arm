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


package scala.resource

import scala.collection.Traversable
import scala.collection.Sequence
import scala.collection.Iterator
import scala.util.control.Exception
import scala.Either


/**
 * This class encapsulates a method of ensuring a resource is opened/closed during critical stages of its lifecycle.
 */
trait ManagedResource[+R] {

  /**
   * This method is used to perform operations on a resource while the resource is open.
   */
  def map[B](f : R => B) : ExtractableManagedResource[B]

  /**
   * This method is used to immediately perform operations on a resource while it is open, ensuring the resource is
   * closed before returning.
   */
  //def flatMap[B](f : H => B) : B    

  /**
   * This method is used to immediately perform operations on a resource while it is open, ensuring the resource is
   * closed before returning.
   */
  def foreach(f : R => Unit) : Unit

  /**
   * Aquires the resource for the Duration of a given function, closing when complete and returning the result.
   *
   * This method will throw the last exception encountered by the managed resource.
   *
   * @param f   A function to execute against the handle returned by the resource
   * @return    The result of the passed in function
   */
  def acquireAndGet[B](f : R => B) : B

  /**
   * Aquires the resource for the Duration of a given function, closing when complete and returning the result.
   *
   * @param f   A function to execute against the handle returned by the resource
   * @return    The result of the function (right) or the list of exceptions seen during the processing of the
   *            resource (left).
   */
  def acquireFor[B](f : R => B) : Either[List[Throwable], B]

  /**
   * This method creates a Traversable in which all performed methods are done withing the context of an "open"
   * resource
   */
  def toTraversable[B](f : R => Iterator[B]) : Traversable[B]

  /**
   * Creates a new resource that is the aggregation of this resource and another.
   *
   * @param that
   *          The other resource
   */
  def and[B](that : ManagedResource[B]) : ManagedResource[(R,B)]

  /**
   *  This method returns a clone of this object where all operations are defered until forced.
   *
   * @returns
   * A lazy version of the ManagedResource
   */
  //def defer : ManagedResource[R,H]
}

/**
 * This trait represents a resource that has been modified (or will be modified) inside an ARM block of some kind.
 */
trait ExtractableManagedResource[+A] {

  /**
   * This method is used to translate a resource further.
   */
  def map[B](f : A => B) : ExtractableManagedResource[B]

  /**
   * This method is used to immediately perform operations on a resource while it is open, ensuring the resource is
   * closed before returning.
   */
  def flatMap[B](f : A => B) : ExtractableManagedResource[B]

  /**
   * This method is used to immediately perform operations on a resource while it is open, ensuring the resource is
   * closed before returning.
   */
  def foreach(f : A => Unit) : Unit

  /** 
   * This method is used to extract the resource being managed.   
   *
   * This allows you to pull information out of the Managed resource, as such, the Resource will not be "available"
   * after this method call.
   *
   * @returns
   *       Some(containedValue) if there have been no processing errors, None otherwise
   */
  def opt : Option[A]

  /**
   * This method is used to extract the resource being managed.
   *
   * This allows you to pull information out of the Managed resource, as such, the Resource will not be "available"
   * after this method call.
   *
   * @returns
   *        An either where the left hand side is the currently contained resource unless exceptions, in which case
   *        the right hand side will contain the sequence of throwable encountered.
   */
  def either : Either[Sequence[Throwable], A]

  /**
   * This method creates a Traversable in which all performed methods are done withing the context of an "open"
   * resource
   */
  def toTraversable[B](f : A => Iterator[B]) : Traversable[B]
}

/**
 * And implementation of a ExtractableManagedResource that defers all processing until the user pulls out information using
 * either or opt functions.
 */
class DeferredExtractableManagedResource[+A,R](val resource : ManagedResource[R], val translate : R => A) extends ExtractableManagedResource[A] { self =>

  override def map[B](f : A => B) = new DeferredExtractableManagedResource(resource, translate.andThen(f))

  override def flatMap[B](f : A => B) =  map(f)

  override def foreach(f : A => Unit) : Unit = resource acquireAndGet translate.andThen(f)

  override def either = resource acquireFor translate

  override def opt = either.right.toOption

  override def toTraversable[B](f : A => Iterator[B]) = resource.toTraversable(translate andThen f)

  override def equals(that : Any) = that match {
    case x : DeferredExtractableManagedResource[A,R] => (x.resource == resource) && (x.translate == translate)
    case _ => false
  }
  override def hashCode() : Int = (resource.hashCode << 7) + translate.hashCode + 13

  override def toString = "DeferredExtractableManagedResource(" + resource + ", " + translate + ")"
}

/**
 * This class implements ManagedResource methods in terms of acquireFor
 */
trait ManagedResourceOperations[+R] extends ManagedResource[R] { self =>
  //TODO - Will the exception list always have size 1?
  override def acquireAndGet[B](f : R => B) : B = acquireFor(f).fold( liste => throw liste.head, x => x)

  override def toTraversable[B](f : R => Iterator[B]) : Traversable[B] = new ManagedTraversable[R,B] {
    val resource = self
    override protected def iterator(resource : R) = f(resource)
  }

  override def map[B](f : R => B) : ExtractableManagedResource[B] = new DeferredExtractableManagedResource(this, f)

  //override def flatMap[B](f : H => B) : B  = acquireAndGet(f)
  override def foreach(f : R => Unit) : Unit = acquireAndGet(f)

  override def and[B](that : ManagedResource[B]) : ManagedResource[(R,B)] = ManagedResource.and(self,that)
}


/**
 * Abstract class implementing most of the managed resource features.
 */
trait AbstractManagedResource[+R,H] extends ManagedResource[R] with ManagedResourceOperations[R] { self =>

  /** 
   * Opens a given resource, returning a handle to execute against during the "session" of the resource being open.
   */
  protected def open : H

  /**
   * Closes a resource using the handle.  This method will throw any exceptions normally occuring during the close of
   * a resource.
   */
  protected def unsafeClose(handle : H) : Unit

  /** 
   * Returns the resource that we are managing.
   */
  protected def translate(handle : H) : R
  /**
   * The list of exceptions that get caught during ARM and will not prevent a call to close.
   */
  protected def caughtException : Sequence[Class[_]] = List(classOf[Throwable])

  override def acquireFor[B](f : R => B) : Either[List[Throwable], B] = {
    import Exception._
    val handle = open
    val result  = catching(caughtException : _*) either (f(translate(handle)))
    val close = catching(caughtException : _*) either unsafeClose(handle)
    //Combine resulting exceptions as necessary     
    result.left.map[List[Throwable]]( _ :: close.left.toOption.toList)
  }
}



/**
 * This class is used when a resource is its own handle.
 */
trait AbstractUntranslatedManagedResource[R] extends AbstractManagedResource[R,R] { self =>
  override protected def translate(handle : R) : R = handle
}

object ManagedResource {
	/**
	 * Creates a ManagedResource for any type with a close method. Note that the opener argument is evaluated on demand,
	 * possibly more than once, so it must contain the code that actually acquires the resource. Clients are encouraged
	 * to write specialized methods to instantiate ManagedResources rather than relying on ad-hoc usage of this method.
	 */
	def apply[A <: { def close() : Unit }](opener : => A) : ManagedResource[A] =
    new AbstractUntranslatedManagedResource[A] {
      override protected def open = opener
			override def unsafeClose(r : A) = r.close()
	  }

  def makeUntranslated[R](opener :  => R)(closer : R => Unit)(nonFatalExceptions : List[Class[_<:Throwable]] = List(classOf[Throwable])) : ManagedResource[R] = 
    new AbstractUntranslatedManagedResource[R] {
      override protected def open = opener
      override protected def unsafeClose(handle : R) : Unit = closer(handle)
      override protected val caughtException = nonFatalExceptions
    }
  /**
   * Creates a new ManagedResource with the given open/close methods
   */
  def make[H, R](opener :  => H)(closer : H => Unit)(translater : H => R, nonFatalExceptions : List[Class[_<:Throwable]] = List(classOf[Throwable])) : ManagedResource[R] = 
    new AbstractManagedResource[R,H] {
      override protected def open = opener
      override protected def translate(handle : H) = translater(handle)
      override protected def unsafeClose(handle : H) : Unit = closer(handle)
      override protected val caughtException = nonFatalExceptions
  }

  /**
   * "ands" two managed resources together.
   */
  def and[A,B](r1 : ManagedResource[A], r2 : ManagedResource[B]) : ManagedResource[(A,B)] = new ManagedResource[(A,B)] with ManagedResourceOperations[(A,B)] {
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
