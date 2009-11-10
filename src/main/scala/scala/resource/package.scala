package scala


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
      override def acquireFor[C](f : ((A,B)) => C) : Either[List[Throwable], C] = {
        val result = r1.acquireFor({ opened1 =>
            r2.acquireFor({ opened2 =>
              f((opened1, opened2))
            })
        })
        result.fold( errors => Left(errors), y => y)      
      }
    }
}

