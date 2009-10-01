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
import scala.collection.Iterable
import scala.util.control.Exception


/**
 * This class encapsulates a method of ensuring a resource is opened/closed during critical stages of its lifecycle.
 */
trait ManagedResource[+R, H] {
  /**
   * This method is used to perform operations on a resource while the resource is open.
   */
  def map[B](f : H => B) : TranslatedResource[R, B]
  //TODO - Should flatmap extract?
  //def flatMap[B](f : H => B) : B    
  //def foreach(f : H => Unit) : Unit

  /**
   * Aquires the resource for the Duration of a given function, closing when complete and returning the result.
   *
   * This method will throw the last exception encountered by the managed resource.
   *
   * @param f   A function to execute against the handle returned by the resource
   * @return    The result of the passed in function
   */
  def acquireAndGet[B](f : H => B) : B
  /**
   * Aquires the resource for the Duration of a given function, closing when complete and returning the result.
   *
   * @param f   A function to execute against the handle returned by the resource
   * @return    The result of the function (right) or the list of exceptions seen during the processing of the resource (left).
   */
  def acquireFor[B](f : H => B) : Either[List[Throwable], B]

  /**
   * This method creates a Traversable in which all performed methods are done withing the context of an "open" resource
   */
  def toTraversable[B](f : H => Iterator[B]) : Traversable[B]

  
  /**
   * This method returns a clone of this object where all operations are defered until forced.
   *
   * @returns
   *        A lazy version of the ManagedResource
   */
  //def defer : ManagedResource[R,H]
}



/**
 * This trait represents a resource that has been modified (or will be modified) inside an ARM block of some kind.
 */
trait TranslatedResource[+R, A] {

  /**
   * This method is used to translate a resource further.
   */
  def map[B](f : A => B) : TranslatedResource[R, B]

  /** 
   * This method is used to extract the resource being managed.   
   *
   * This allows you to pull information out of the Managed resource, as such, the Resource will not be "available" after this method call.
   *
   * @returns
   *       Some(containedValue) if there have been no processing errors, None otherwise
   */
  def opt : Option[A]
  /**
   * This method is used to extract the resource being managed.
   *
   * This allows you to pull information out of the Managed resource, as such, the Resource will not be "available" after this method call.
   *
   * @returns
   *        An either where the left hand side is the currently contained resource unless exceptions, in which case the right hand side will contain the sequence of throwable encountered.
   */
  def either : Either[Sequence[Throwable], A]

  /**
   * This method creates a Traversable in which all performed methods are done withing the context of an "open" resource
   */
  def toTraversable[B](f : A => Iterator[B]) : Traversable[B]
}

/**
 * And implementation of a TranslatedResource that defers all processing until the user pulls out information using either or opt functions.
 */
class DeferredTranslatedResource[+R,H,A](val resource : ManagedResource[R,H], val translate : H => A) extends TranslatedResource[R,A] { self =>

   override def map[B](f : A => B) = new DeferredTranslatedResource(resource, translate.andThen(f))

   override def either = resource acquireFor translate

   override def opt = either.right.toOption

   override def toTraversable[B](f : A => Iterator[B]) = resource.toTraversable(translate andThen f)
}

/**
 * Abstract class implementing most of the managed resource features.
 */
trait AbstractManagedResource[R,H] extends ManagedResource[R,H] { self =>
  /** 
   * Opens a given resource, returning a handle to execute against during the "session" of the resource being open.
   */
  protected def open(resource : R) : H
  /**
   * Closes a resource using the handle.  This method will throw any exceptions normally occuring during the close of a resource.
   */
  protected def unsafeClose(handle : H) : Unit
  /** 
   * Returns the resource that we are managing.
   */
  protected def resource : R
  /**
   * The list of exceptions that get caught during ARM and will not prevent a call to close.
   */
  protected def caughtException : Sequence[Class[_]] = List(classOf[Throwable])


  override def acquireFor[B](f : H => B) : Either[List[Throwable], B] = {
     import Exception._
     val handle = open(resource)
     val result  = catching(caughtException : _*) either (f(handle))
     val close = catching(caughtException : _*) either unsafeClose(handle)
     //Combine resulting exceptions as necessary     
     result.left.map[List[Throwable]]( _ :: close.left.toOption.toList)
  }
  //TODO - Will the exception list always have size 1?
  override def acquireAndGet[B](f : H => B) : B = acquireFor(f).fold( liste => throw liste.head, x => x)

  override def toTraversable[B](f : H => Iterator[B]) : Traversable[B] = new ManagedTraversable[R,H,B] {
     val resource = self
     override protected def iterator(handle : H) = f(handle)
  }

  override def map[B](f : H => B) : TranslatedResource[R, B] = new DeferredTranslatedResource(this, f)
  
}

/**
 * This class is used when a resource is its own handle.
 */
trait AbstractNoHandleManagedResource[R] extends AbstractManagedResource[R,R] { self =>
   override protected def open(resource : R) = resource
}

object ManagedResource {
	/** Creates a ManagedResource for any type with a close method. Note that
	 * the opener argument is evaluated on demand, possibly more than once, so
	 * it must contain the code that actually acquires the resource. Clients
	 * are encouraged to write specialized methods to instantiate
	 * ManagedResources rather than relying on ad-hoc usage of this method. */
	def apply[A <: { def close() : Unit }](opener : => A) : ManagedResource[A,A] =
		new AbstractNoHandleManagedResource[A] {
			override def resource = opener
			override def unsafeClose(r : A) = r.close()
	}
	/**
         * Creates a new ManagedResource with the given open/close methods
         */
        def make[H, R](r : R, opener : R => H, closer : H => Unit, nonFatalExceptions : List[Class[_<:Throwable]] = List(classOf[Throwable])) : ManagedResource[R,H] = new AbstractManagedResource[R,H] {
           override val resource = r
           override protected def open(res : R) = opener(res)
	   override protected def unsafeClose(handle : H) : Unit = closer(handle)
           override protected val caughtException = nonFatalExceptions
        }
}
