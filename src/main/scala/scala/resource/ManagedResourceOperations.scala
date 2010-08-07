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
import _root_.scala.collection.TraversableOnce
import _root_.scala.util.continuations.{cps,shift, suspendable}
/**
 * This class implements all ManagedResource methods except acquireFor.   This allows all new ManagedResource
 * implementations to be defined in terms of the acquireFor method.
 */
trait ManagedResourceOperations[+R] extends ManagedResource[R] { self =>
  //TODO - Can we always grab the top exception?
  override def acquireAndGet[B](f : R => B) : B = acquireFor(f).fold( liste => throw liste.head, x => x)

  override def toTraversable[B](f : R => TraversableOnce[B]) : Traversable[B] = new ManagedTraversable[B,R] {
    val resource = self
    override protected def internalForeach[U](resource: R, g : B => U) : Unit = f(resource).foreach(g) 
  }

  override def map[B, To](f : R => B)(implicit translator : CanSafelyMap[B,To]) : To = translator(self,f)

  override def flatMap[B, To](f : R => B)(implicit translator : CanSafelyFlatMap[B,To]) : To = translator(self,f)
  
  override def foreach(f : R => Unit) : Unit = acquireAndGet(f)

  override def and[B](that : ManagedResource[B]) : ManagedResource[(R,B)] = resource.and(self,that)

  override def reflect[B] : R @cps[Either[List[Throwable], B]] = shift {
    k : (R => Either[List[Throwable],B]) =>
      acquireFor(k).fold(list => Left(list), identity)
  }
  // Some wierd intersection of scaladoc2 + continuations plugin forces us to be explicit about types here!
  override def ! : R @suspendable = shift { (k : R => Unit) =>
    acquireAndGet(k)
    ()
  } 
}



