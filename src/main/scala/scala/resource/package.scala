package scala

import resource.{ManagedResourceOperations, AbstractManagedResource, AbstractUntranslatedManagedResource, ManagedResource}

package object resource {
	  /**
	   * Creates a ManagedResource for any type with a close method. Note that the opener argument is evaluated on demand,
	   * possibly more than once, so it must contain the code that actually acquires the resource. Clients are encouraged
	   * to write specialized methods to instantiate ManagedResources rather than relying on ad-hoc usage of this method.
	   */
	  def managed[A <: { def close() : Unit }](opener : => A) : ManagedResource[A] =
      new AbstractUntranslatedManagedResource[A] {
        override protected def open = opener
			  override def unsafeClose(r : A) = r.close()
	    }

    def makeUntranslatedManagedResource[R](opener :  => R)(closer : R => Unit)(nonFatalExceptions : List[Class[_<:Throwable]] = List(classOf[Throwable])) : ManagedResource[R] =
      new AbstractUntranslatedManagedResource[R] {
        override protected def open = opener
        override protected def unsafeClose(handle : R) : Unit = closer(handle)
        override protected val caughtException = nonFatalExceptions
      }
    /**
     * Creates a new ManagedResource with the given open/close methods
     */
    def makeManagedResource[H, R](opener :  => H)(closer : H => Unit)(translater : H => R, nonFatalExceptions : List[Class[_<:Throwable]] = List(classOf[Throwable])) : ManagedResource[R] =
      new AbstractManagedResource[R,H] {
        override protected def open = opener
        override protected def translate(handle : H) = translater(handle)
        override protected def unsafeClose(handle : H) : Unit = closer(handle)
        override protected val caughtException = nonFatalExceptions
    }

    /**
     * "ands" two managed resources together.
     */
    def and[A,B](r1 : ManagedResource[A], r2 : ManagedResource[B]) : ManagedResource[(A,B)] = new ManagedResource[(A,B)] with ManagedResourceOperations[(A,B)] {
      import scala.util.continuations._
      override def acquireFor[C](f : ((A,B)) => C) : Either[List[Throwable], C] = {
        val result = reset {
          val resource1 = r1.reflect[Either[List[Throwable],C]]
          val resource2 = r2.reflect[C]
          f(resource1,resource2)
        }
        result.fold(x => Left(x), y => y)
      }
    }
}

