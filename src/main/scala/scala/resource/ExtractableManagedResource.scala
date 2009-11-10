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

import collection.Sequence

/**
 * This trait represents a resource that has been modified (or will be modified) inside an ARM block in such
 * a way that the resulting value can be extracted outside of the "ManagedResource" monad.
 */
trait ExtractableManagedResource[+A] extends ManagedResource[A] {


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
}
