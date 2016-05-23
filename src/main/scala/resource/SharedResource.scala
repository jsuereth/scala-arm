package resource

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger

final class SharedResource[A : Resource : Manifest](opener: => A) extends AbstractManagedResource[A] {
	private val resource = implicitly[Resource[A]]
	// A referencing counting atomic variable we use to "guard" this resource.
	// Currently, we spin-lock accessing this ref because we don't expect opening to block a thread for very long.
	private val ref = new AtomicReference[Option[A]](None)
	private val count = new AtomicInteger(0)

	protected def open: A = {
		val myCount = count.addAndGet(1)
		// If we win the race, we construct the resource
		if (myCount == 1) {
			// We must construct the reference
			// We may want to CAS here and fail if we're inconsistent.
			val r = opener
			ref.lazySet(Some(r))
			resource open r
		}
		// Lazy spin to get the current value
		def get(): A = 
		   ref.get match {
		     case Some(r) => r
		     case None => 
		       // TODO - backoff strategy
		       Thread.`yield`()
		       get()
		   }
		get()
	}
	protected def unsafeClose(handle: A, errors: Option[Throwable]): Unit = {
		val cnt = count.decrementAndGet()
		// TODO - Throw error if cnt reaches negative?
		if (cnt == 0) {
			// Mark the resource as closed, while we clean up behind the scenes.
			// If the CAS fails, it means someone else has already opened the resource again.
			ref.compareAndSet(Some(handle), None)
			// Cleanup the resource
			// TODO - We don't know if an error occurred on a different thread, so we need to figure out how to flag that.
			//        We porbably need some kind of atomic reference of failure on a thread so we can call this appropriately.
			errors match {
              case None    => resource.close(handle)
              case Some(t) => resource.closeAfterException(handle, t)
            }
		}

	}

  override protected def isFatal(t: Throwable): Boolean =
    resource isFatalException t
  override protected def isRethrown(t: Throwable): Boolean =
    resource isRethrownException t
  /* You cannot serialize resource and send them, so referential equality should be sufficient. */
  override def hashCode(): Int = (resource.hashCode << 7) + super.hashCode + 13
	override def toString = s"SharedResource[${implicitly[Manifest[A]]}]"
}