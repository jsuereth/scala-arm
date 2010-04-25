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
 * This trait's existence signifies that a ManagedResource can be converted into a another type safely or needs to remaining inside the monad.
 */
trait CanSafelyTranslate[-MappedElem, +To] {
  /**
   * This method takes a managed resource and a mapping function and returns a new result (inside/outside the managed resource).
   */
  def apply[T](from : ManagedResource[T],  converter : T => MappedElem) : To
}



trait LowPriorityManagedResourceImplicits {
  /** This translate method converts from ManagedResource to ExtractableManagedResource, keeping the processed result inside the managed monad. */
  implicit def stayManaged[B] = new CanSafelyTranslate[B, ExtractableManagedResource[B]] {
    def apply[A](from : ManagedResource[A],  converter : A => B) : ExtractableManagedResource[B] =  new DeferredExtractableManagedResource(from, converter)
  }
}

trait HighPriorityManagedResourceImplicits extends LowPriorityManagedResourceImplicits {

  /** Assumes any mapping function to an iterator type creates a "traversable" */
  implicit def convertToTraversable[A, CC[X] <: Traversable[X]] = new CanSafelyTranslate[CC[A], Traversable[A]] {
     override def apply[T](from : ManagedResource[T],  converter : T => CC[A]) : Traversable[A] = new ManagedTraversable[A,CC[A]] {
        override val resource : ManagedResource[CC[A]] = from.map(converter)(stayManaged)
        override protected def internalForeach[U](r: CC[A], f : A => U) : Unit = r.foreach(f)
     }
  }

  /** Assumes any mapping can remain in monad */
  implicit def stayManagedFlatten[B] = new CanSafelyTranslate[ManagedResource[B], ManagedResource[B]] {
      def apply[A](from : ManagedResource[A],  converter : A => ManagedResource[B]) : ManagedResource[B] = new ManagedResourceOperations[B] {
          override def acquireFor[C](f2 : B => C) : Either[List[Throwable], C] = {
            from.acquireFor( r => converter(r).acquireFor(f2)).fold(x => Left(x), x => x)
          }
      }
  }
}

object CanSafelyTranslate extends HighPriorityManagedResourceImplicits

