package resource

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger


/**
 * Shared resources are an attempt to share a resourcce between threads IFF the underlying resource is threadsafe.
 *  Shared resources will open the resource the first time `open` is called, and close the resource after the reference
 *  count has returned to zero.
 *
 *  On failure - The resource will be immediately closed.  Any future open request will attempt to open a new
 *               resource for sharing.  Existing users of a failed resource will have thier close methods silently
 *               do nothing, but we have no way of notifying them of the failure
 *
 * Intended usage - TODO - write this
 *                         
 */
final class SharedResource[A <: AnyRef : Resource : Manifest](opener: => A) extends AbstractManagedResource[A] {
	private def resource = implicitly[Resource[A]]

	private object resourceShare {
		private var ref: A = null.asInstanceOf[A]
		private var count: Int = 0
		def open: A = synchronized {
			if (ref == null) {
				ref = opener
				resource.open(ref)
			}
			count += 1
			ref
		}
		def flagError(ref: A, t: Throwable): Unit = synchronized {
			if (ref eq this.ref) {
			  count = 0
			  this.ref = null.asInstanceOf[A]
			  resource.closeAfterException(ref, t)
			}

		}
		def close(ref: A): Unit = synchronized {
			if (this.ref eq ref) {
				count -= 1
				if (count == 0) {
					resource.close(ref)
					this.ref = null.asInstanceOf[A]
				}
			}
		}
	}

	protected def open: A = resourceShare.open
	protected def unsafeClose(handle: A, errors: Option[Throwable]): Unit = {
		errors match {
          case None    => resourceShare.close(handle)
          case Some(t) => resourceShare.flagError(handle, t)
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