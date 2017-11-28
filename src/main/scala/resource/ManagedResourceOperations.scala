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

import _root_.scala.collection.{Traversable, TraversableOnce}
import _root_.scala.concurrent.{ ExecutionContext, Future }

/**
 * This class implements all ManagedResource methods except acquireFor.   This allows all new ManagedResource
 * implementations to be defined in terms of the acquireFor method.
 */
trait ManagedResourceOperations[+R] extends ManagedResource[R] { self =>
  //TODO - Can we always grab the top exception?
  override def acquireAndGet[B](f: R => B): B = apply(f)
  override def apply[B](f: R => B): B = acquireFor(f).fold( 
    liste => throw liste reduce { (prev, next) =>
      prev.addSuppressed(next)
      prev
    }, 
    x => x)

  override def toTraversable[B](implicit ev: R <:< TraversableOnce[B]): Traversable[B] = 
    new ManagedTraversable[B,R] {
      val resource = self
      override protected def internalForeach[U](resource: R, g : B => U) : Unit = 
        ev(resource).foreach(g) 
    }

  override def toFuture(implicit context: ExecutionContext): Future[R] = 
    Future(acquireAndGet(identity))

  override def map[B](f : R => B): ExtractableManagedResource[B] =
    new DeferredExtractableManagedResource(self, f)
  
  override def flatMap[B](f : R => ManagedResource[B]): ManagedResource[B] =
    new ManagedResourceOperations[B] {
      override def acquireFor[C](f2 : B => C) : ExtractedEither[List[Throwable], C] = {
        var extracted: ExtractedEither[List[Throwable], C] = ExtractedEither(Left(Nil))
        self.acquireFor { r =>
          extracted = f(r).acquireFor(f2)
          extracted.fold(a => throw a.head, _ => ())
        }.fold(x => ExtractedEither(Left(extracted.fold(_ ++ x.tail, _ => x))), _ => extracted)
      }

	  override def toString = "FlattenedManagedResource[?](...)"
    }
  override def foreach(f: R => Unit): Unit = acquireAndGet(f)
  override def and[B](that: ManagedResource[B]) : ManagedResource[(R,B)] = resource.and(self,that)
}



