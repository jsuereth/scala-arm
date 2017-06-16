// -----------------------------------------------------------------------------
//
//  scala.arm - The Scala Incubator Project
//  Copyright (c) 2009 and onwards The Scala Incubator Project. All rights reserved.
//
//  The primary distribution site is http://jsuereth.github.com/scala-arm
//
//  This software is released under the terms of the Revised BSD License.
//  There is NO WARRANTY.  See the file LICENSE for the full text.
//
// -----------------------------------------------------------------------------


package resource

import _root_.scala.collection.Traversable
import _root_.scala.collection.Iterator
import _root_.scala.Either
import _root_.scala.concurrent.{ ExecutionContext, Future }

/**
 * This class encapsulates a method of ensuring a resource is opened/closed during critical stages of its lifecycle.
 * It is monadic in nature, although not a monad, and provides several combinators to use with other managed resources.
 *
 * For example:
 * <pre>
 * val x = managed(newResource)
 * val y = managed(newResource)
 * val z : ManagedResource[Z] = x and y map { case (x,y) => f(x,y) }
 * </pre>
 * 
 */
trait ManagedResource[+R] {

  /**
   * This method is used to perform operations on a resource while the resource is open.
   *
   * @param f The transformation function to apply against the raw resource.
   *
   * @return A new ManagedResource with the translated type or some other type if an appropriate translator was found.
   */
  def map[B](f: R => B): ExtractableManagedResource[B]

  /**
   * This method is used to immediately perform operations on a resource while it is open, ensuring the resource is
   * closed before returning.
   *
   * @param f The transformation function to apply against the raw resource.
   *
   * @return A new ManagedResource with the translated type or some other type if an appropriate translator was found.
   *
   * @usecase def flatMap[B](f: R => B): ManagedResource[B]
   * @usecase def flatMap[B](f : R => Traversable[B]) : Traversable[B]
   * @usecase def flatMap[B](f : R => ManagedResource[B]) : ManagedResource[B]
   */ 
  def flatMap[B](f: R => ManagedResource[B]): ManagedResource[B]

  /**
   * This method is used to immediately perform operations on a resource while it is open, ensuring the resource is
   * closed before returning.   Note:  This method *will* open and close the resource, performing the body of the
   * method immediately.
   *
   * @param f The function to apply against the raw resource.
   */
  def foreach[U](f: R => U) : Unit

  /**
   * Acquires the resource for the Duration of a given function, The resource will automatically be opened and closed.
   * The result will be returned immediately, except in the case of an error.   Upon error, the resource will be
   * closed, and then the originating exception will be thrown.
   *
   * Note: This method will throw the last exception encountered by the managed resource, whatever this happens to be.
   *
   * @param f   A function to execute against the handle returned by the resource
   * @return    The result of the passed in function
   */
  def acquireAndGet[B](f: R => B): B

  /**
   * Acquires the resource for the Duration of a given function, The resource will automatically be opened and closed.
   * The result will be returned immediately, except in the case of an error.   Upon error, the resource will be
   * closed, and then the originating exception will be thrown.
   *
   * Note: This method will throw the last exception encountered by the managed resource, whatever this happens to be.
   *
   * @param f   A function to execute against the handle returned by the resource
   * @return    The result of the passed in function
   */
  def apply[B](f: R => B): B

  /**
   * Aquires the resource for the Duration of a given function, The resource will automatically be opened and closed.
   * The result will be returned immediately in an Either container.   This container will hold all errors, if any
   * occurred during execution, or the resulting value.
   *
   * @param f   A function to execute against the raw resource.
   * @return    The result of the function (right) or the list of exceptions seen during the processing of the
   *            resource (left).
   */
  def acquireFor[B](f: R => B): ExtractedEither[List[Throwable], B]

  /**
   * This method creates a Traversable in which all performed methods are done within the context of an "open"
   * resource.   Note:  Every iteration will attempt to open and close the resource!
   *
   * @param f  A function that transforms the raw resource into an Iterator of elements of type B.
   *
   * @return A Traversable of elements of type B. 
   */
  def toTraversable[B](implicit ev: R <:< TraversableOnce[B]): Traversable[B]

  /**
   * This method creates a Future that will perform operations  
   * within the context of an "open" resource. 
   * Execution of Future will hold error as Failure, otherwise result will be 
   * inside a Success.
   */
  def toFuture(implicit context: ExecutionContext): Future[R]

  /**
   * Creates a new resource that is the aggregation of this resource and another.
   *
   * @param that
   *          The other resource
   *
   * @return
   *          A resource that is a tupled combination of this and that.
   */
  def and[B](that: ManagedResource[B]): ManagedResource[(R,B)]
}




