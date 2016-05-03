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

import collection.Seq
import util.Try

/**
 * This trait represents a resource that has been modified (or will be modified) inside an ARM block in such
 * a way that the resulting value can be extracted outside of the "ManagedResource" monad.  There are two mechanisms
 * for extracting resources.  One which returns an optional value, where None is returned if any error occurs during
 * extraction.   The other returns an Either where the left side contains any error that occured during extraction
 * and the right side contains the extracted value.
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
  def opt: Option[A]

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
  def either: ExtractedEither[Seq[Throwable], A]

  /**
   * This method is used to extract the resource being managed.
   *
   * This allows you to pull information out of the Managed resource, as such, the reosurce will not be "available"
   * after this method call.
   *
   * @returns
   *        A [[scala.util.Try]] instance, which is [[scala.util.Success]] if there were no exceptions pulling out the 
   *        resource, or a [[scala.util.Failure]] if there were.  In the event of multiple failures, they will
   *        be added to the supressed exception list of the resulting Failure.
   */
  def tried: Try[A]
}