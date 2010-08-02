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

package scala.resource

/**
 * This trait's existence signifies that a ManagedResource can be "flatMap"ed into a another type safely or needs to
 * remain inside the monad.  This type class applies to all flatMap calls.
 *
 * This applies to all "map" calls.
 */
trait CanSafelyFlatMap[-MappedElem, +To] {
  /**
   * This method takes a managed resource and a mapping function and returns a new result
   * (inside/outside the managed resource).
   */
  def apply[T](from : ManagedResource[T],  converter : T => MappedElem) : To
}

/**
 * This trait's existence signifies that a ManagedResource can be converted to some type safely, or needs
 * to remain inside the monad.   This type class applies to all map calls.
 */
trait CanSafelyMap[-MappedElem, +To] {
  /**
   * This method takes a managed resource and a mapping function and returns a new result
   * (inside/outside the managed resource).
   */
  def apply[T](from : ManagedResource[T],  converter : T => MappedElem) : To
}

/**
 * Contains the low priority implicits for CanSafelyTranslate.   i.e. When manipulating a resource using map or flatMap
 * this is the very last rule that is checked.   As such, it contains the "any type we don't know about" rule, which is
 * to return an "Extractable" managed resource.
 */
trait LowPriorityCanSafelyMapImplicits {
  /**
   * This translate method converts from ManagedResource to ExtractableManagedResource, keeping the processed
   * result inside the managed monad.
   */
  implicit def stayManaged[B] = new CanSafelyMap[B, ExtractableManagedResource[B]] {
    def apply[A](from : ManagedResource[A],  converter : A => B) : ExtractableManagedResource[B] =
      new DeferredExtractableManagedResource(from, converter)
  }
}

/**
 * Contains medium priority implicits for CanSafelyTranslate.   This contains the implicits that will detect
 * when you are mapping or flattening a resource into another resource and return another ManagedResource of
 * the new type.   THis relies on the Resource type trait.
 */
trait HighPrioritCanSafelyMapImplicits extends LowPriorityCanSafelyMapImplicits {
  /** Assumes any mapping function to an iterator type creates a "traversable" */
  implicit def convertToTraversable[A : Manifest] = new CanSafelyMap[Traversable[A], Traversable[A]] {
     override def apply[T](from : ManagedResource[T],  converter : T => Traversable[A]) : Traversable[A] =
       new ManagedTraversable[A,Traversable[A]] {
         override val resource : ManagedResource[Traversable[A]] = from.map(converter)(stayManaged)
         override protected def internalForeach[U](r: Traversable[A], f : A => U) : Unit = r.foreach(f)
         override def toString = "ManagedTraversable["+implicitly[Manifest[A]]+"](...)"
       }
  }
}

/**
 * Contains higher priority translations for ManagedResource map and flatMap calls.   These include conversions
 * from one managed resource to a nested managed resource, or converting into a Traversable of some type.   Both
 * of these calls can be optimized.
 */
trait LowPrioritCanSafelyFlatMapImplicits {

  /** This will flatMap such that the new resource will be flattened. */
  implicit def stayManagedFlatten[B : Manifest] = new CanSafelyFlatMap[ManagedResource[B], ManagedResource[B]] {
      def apply[A](from : ManagedResource[A],  converter : A => ManagedResource[B]) : ManagedResource[B] =
        new ManagedResourceOperations[B] {
          override def acquireFor[C](f2 : B => C) : Either[List[Throwable], C] = {
            from.acquireFor( r => converter(r).acquireFor(f2)).fold(x => Left(x), x => x)
          }
          override def toString = "FlattenedManagedResource[" + implicitly[Manifest[B]] + "](...)"
        }
  }
  /**
   * Returns a new translate method that will nest a new resource beneath the one used to create it.
   */
  implicit def flattenNestedResource[B : Resource : Manifest] = new CanSafelyFlatMap[B, ManagedResource[B]] {
    def apply[A](from : ManagedResource[A], converter : A => B) : ManagedResource[B] =
      new ManagedResourceOperations[B] {
        override def acquireFor[C](f : B => C) : Either[List[Throwable], C] = {
          from.acquireFor({ r =>
            managed(converter(r)).acquireFor(f)
          }).fold(x => Left(x), x => x)
        }
        override def toString = "AutoFlattenedManagedResource[" + implicitly[Manifest[B]] + "](...)"
      }
  }
}

/** This companion object contains implicits used on ManagedResource.flatMap calls. */
object CanSafelyFlatMap extends LowPrioritCanSafelyFlatMapImplicits {
    /** Assumes any mapping function to an iterator type creates a "traversable" */
  implicit def convertToTraversable[A : Manifest] = new CanSafelyFlatMap[Traversable[A], Traversable[A]] {
     override def apply[T](from : ManagedResource[T],  converter : T => Traversable[A]) : Traversable[A] =
       new ManagedTraversable[A,Traversable[A]] {
         override val resource : ManagedResource[Traversable[A]] = from.map(converter)
         override protected def internalForeach[U](r: Traversable[A], f : A => U) : Unit = r.foreach(f)
         override def toString = "ManagedTraversable["+implicitly[Manifest[A]]+"](...)"
       }
  }
}

/**
 * This companion object contains implicits used on ManagedResource map calls.
 */
object CanSafelyMap extends HighPrioritCanSafelyMapImplicits {
  /** 
   * This method can be used to extract from a ManagedResource some value while mapping/flatMapping.
   *  e.g. <pre> val x : ManagedResource[Foo]
   *   val someValue = x.flatMap( foo : Foo => foo.getSomeValue )(extractUnManaged)
   * </pre>
   */
  def extractUnManaged[T] = new CanSafelyMap[T, T] {
     def apply[A](from : ManagedResource[A], converter : A => T) : T = from.acquireAndGet(converter)
  }
  /**
   * This method can be used to extract from a ManagedResource some value while mapping/flatMapping.
   *  e.g. <pre> val x : ManagedResource[Foo]
   *   val someValue : Option[SomeValue] = x.flatMap( foo : Foo => foo.getSomeValue )(extractOption)
   * </pre>
   */
  def extractOption[T] = new CanSafelyMap[T, Option[T]] {
     def apply[A](from : ManagedResource[A], converter : A => T) : Option[T] = (new DeferredExtractableManagedResource(from, converter)).opt
  }
}


