package scala
package resource
package jndi

import java.util.concurrent.Future
import annotation.tailrec
import javax.naming.{NameNotFoundException, InitialContext, Context}
import java.lang.Thread
import javax.rmi.PortableRemoteObject


/** Represents a timeout in our blocking access to a resource */
class TimeoutException(msg: String) extends javax.naming.NamingException(msg) {}

/**
 * Blocking access to a JNDI Resource
 */

class BlockingJndiResource[+R : Manifest](ctx : => Context, name : String, maxTries : Int = 10, initialdelayms : Long = 100L, backoffms : Long = 100L) extends ManagedResource[R] with ManagedResourceOperations[R]  {

  /** Acquires a context to check availability for a reosurce... */
  def context : Context = ctx

  /**
   * Aquires the resource for the Duration of a given function, closing when complete and returning the result.
   *
   * @param f   A function to execute against the handle returned by the resource
   * @return    The result of the function (right) or the list of exceptions seen during the processing of the
   *            resource (left).
   */
  def acquireFor[B](f : R => B) : ErrorHolder[B] = {

      def lookUp : R = PortableRemoteObject.narrow(context.lookup(name), implicitly[Manifest[R]].erasure).asInstanceOf[R]

      var attemptCount = 1
      while(attemptCount <= maxTries) {
        try {
          Right(f(lookUp))
        } catch {
          case _ : NameNotFoundException =>
           import scala.util.control.Exception._
           ignoring(classOf[InterruptedException]) opt (Thread.sleep(initialdelayms + (backoffms*attemptCount)))
        }
        attemptCount += 1
      }
      Left(List(new TimeoutException("Could not locate: " + name + " within " + maxTries + " attempts")))      
  }
}


object BlockingJndiResource extends HighPriorityManagedResourceImplicits {
  def make[A : Manifest](name : String, context : => Context = new InitialContext) = new BlockingJndiResource[A](context, name)
}
