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

/**
 * This trait's existence signifies that a ManagedResource can be converted into a ManagedTraversable safely or needs to remaining inside the monad.
 */
trait CanSafelyTranslate[-MappedElem, +To] {
  /**
   * This method takes a managed resource and a mapping function and returns a new result (inside/outside the managed resource).
   */
  def apply[T](from : ManagedResource[T],  converter : T => MappedElem) : To
}

