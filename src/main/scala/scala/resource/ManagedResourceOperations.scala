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
import _root_.scala.util.continuations.{cps,cpsParam,shift}
/**
 * This class implements ManagedResource methods in terms of acquireFor
 */
trait ManagedResourceOperations[+R] extends ManagedResource[R] { self =>
  //We need our helper methods
  import ManagedResource._
  
  //TODO - Will the exception list always have size 1?
  override def acquireAndGet[B](f : R => B) : B = acquireFor(f).fold( liste => throw liste.head, x => x)

  override def toTraversable[B](f : R => Iterator[B]) : Traversable[B] = new ManagedTraversable[B,R] {
    val resource = self
    override protected def internalForeach[U](resource: R, g : B => U) : Unit = f(resource).foreach(g) 
  }

  override def map[B, To](f : R => B)(implicit translator : CanSafelyTranslate[B,To]) : To = translator(self,f)

  override def flatMap[B, To](f : R => B)(implicit translator : CanSafelyTranslate[B,To]) : To = translator(self,f)
  
  override def foreach(f : R => Unit) : Unit = acquireAndGet(f)

  override def and[B](that : ManagedResource[B]) : ManagedResource[(R,B)] = resource.and(self,that)


  override def reflect2[B] : R @cps[Either[List[Throwable], B]] = shift {
    k : (R => Either[List[Throwable],B]) =>

            //Either[List[Throwable],Either[List[Throwable],B]]
      acquireFor(k).fold(list => Left(list), identity)
  }

  override def reflect[B] : R @cpsParam[B,Either[List[Throwable], B]] = shift(acquireFor)

  override def reflectUnsafe[B] : R @cps[B] = shift(acquireAndGet)
}



