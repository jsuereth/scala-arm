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

import _root_.scala.collection.Traversable
import _root_.scala.collection.Iterator
import _root_.scala.util.control.Exception
import _root_.scala.Either
import util.continuations.{cpsParam, cps}

/**
 * This class encapsulates a method of ensuring a resource is opened/closed during critical stages of its lifecycle.
 */
trait ManagedResource[+R] {

  /**
   * This method is used to perform operations on a resource while the resource is open.
   */
  def map[B, To](f : R => B)(implicit translator : CanSafelyMap[B,To]) : To
  //def map[B](f : R => B) : ExtractableManagedResource[B]

  /**
   * This method is used to immediately perform operations on a resource while it is open, ensuring the resource is
   * closed before returning.
   */
  //def flatMap[B](f : R => ManagedResource[B]) : ManagedResource[B]    
  def flatMap[B, To](f : R => B)(implicit translator : CanSafelyFlatMap[B,To]) : To

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
   * Reflects the resource for use in a continuation.
   */
  def reflect[B] : R @cps[Either[List[Throwable], B]]

  /**
   * Reflects the resource for use in a continuation.  Exceptions will not be stored in an Either, but thrown after
   * resource management is complete
   */
  def reflectUnsafe[B] : R @cps[B]
}




