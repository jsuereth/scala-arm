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

/**
 * This class implements ManagedResource methods in terms of acquireFor
 */
trait ManagedResourceOperations[+R] extends ManagedResource[R] { self =>
  //We need our helper methods
  import ManagedResource._
  
  //TODO - Will the exception list always have size 1?
  override def acquireAndGet[B](f : R => B) : B = acquireFor(f).fold( liste => throw liste.head, x => x)

  override def toTraversable[B](f : R => Iterator[B]) : Traversable[B] = new ManagedTraversable[R,B] {
    val resource = self
    override protected def iterator(resource : R) = f(resource)
  }

  //override def map[B, To](f : R => B)(implicit translator : CanSafelyTranslate[B,To]) : To = translator(self,f)
  override def map[B](f : R => B) : ExtractableManagedResource[B] = flatMap(f)(stayManaged)

  /*override def flatMap[B](f : R => ManagedResource[B]) : ManagedResource[B] = new ManagedResourceOperations[B] {
      override def acquireFor[C](f2 : B => C) : Either[List[Throwable], C] = {
        self.acquireFor( r => f(r).acquireFor(f2)).fold(x => Left(x), x => x)
      }
    }*/
  override def flatMap[B, To](f : R => B)(implicit translator : CanSafelyTranslate[B,To]) : To = translator(self,f)
  
  override def foreach(f : R => Unit) : Unit = acquireAndGet(f)

  override def and[B](that : ManagedResource[B]) : ManagedResource[(R,B)] = resource.and(self,that)
}



