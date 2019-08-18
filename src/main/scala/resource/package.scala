/**
 * Package related methods for managed resources.
 */
package object resource {

  /** The type used to hold errors and results in the same return value. */
  type ErrorHolder[A] = Either[List[Throwable],A]
  /**
   * Creates a ManagedResource for any type with a Resource type class implementation.   This includes all
   * java.lang.AutoCloseable subclasses, and any types that have a close or dispose method.  You can also provide your own
   * resource type class implementations in your own scope.
   */
  def managed[A : Resource : OptManifest](opener: => A) : ManagedResource[A] = new DefaultManagedResource(opener)

  /**
    * Use this to encapsulate a constant value inside a ManagedResource with no resource management.
    */
  def constant[V](value: V) : ManagedResource[V] = new ConstantManagedResource(value)

  /**
   * Constructs a managed resource using function objects for each abstract method.
   *
   * @param opener  The by-name parameter that will open the resource.
   * @param closer  A closure that will close the resource.
   * @param exceptions  A list of exception classes that cannot be ignored to close a resource.
   */
  def makeManagedResource[R : OptManifest](opener:  => R)(closer: R => Unit)(exceptions: List[Class[_<:Throwable]]) = {
    implicit val typeTrait: Resource[R] = new Resource[R] {
      override def close(r : R) = closer(r)
      override def isFatalException(t: Throwable): Boolean =
        exceptions exists { cls => cls.isAssignableFrom(t.getClass) }
    }
    new DefaultManagedResource(opener)
  }

  /**
   * Combined two resources such that they are both opened/closed together.   The first resource is opened before
   * the second resource and closed after the second resource, however the resulting ManagedResource acts like
   * both are opened/closed together.
   * @return A ManagedResource of a tuple containing the initial two resources.
   */
  def and[A,B](r1: ManagedResource[A], r2: ManagedResource[B]): ManagedResource[(A,B)] =
    r1 flatMap { ther1 =>
      r2 map { ther2 => (ther1, ther2) }
    }

  /**
   * Takes a sequence of ManagedResource objects and traits them as a ManagedResource of a Sequence of Objects.
   *
   * This is useful for dealing with many resources within the same scope.
   *
   * @param resources   A collection of ManageResources of the same type
   * @return  A ManagedResoruce of a collection of types
   */
  def join[A, MR, CC](resources: CC)(implicit ev0: CC <:< Seq[ MR ], ev1: MR <:< ManagedResource[A]): ManagedResource[Seq[A]] = {
    //TODO - Use foldLeft
    //TODO - Don't use such a sucky algorithm...
    //We currently assume 1 resource
    //TODO - See if we can provide Hlist implementation as well...
    val itr = (resources.reverseIterator: Iterator[MR])
    val first : ManagedResource[A] = itr.next
    var toReturn: ManagedResource[Seq[A]] = first.map( x => Seq(x))
    while(itr.hasNext) {
      val r1 = toReturn
      val r2: ManagedResource[A] = itr.next
      toReturn = new ManagedResource[Seq[A]] with ManagedResourceOperations[Seq[A]] {
        override def acquireFor[B](f : Seq[A] => B) : ExtractedEither[List[Throwable], B] = r1.acquireFor {
          r1seq =>
            r2.acquireAndGet { r2item =>
              f( r2item :: r1seq.toList)
            }
        }
      }
    }
    toReturn
  }

  /**
    * Creates a [[ManagedResource]] shared by all users for any type with a [[Resource]] type class implementation.
    *
    * There is only one instance of the resource at the same time for all the users.
    * The instance will be closed once no user is still using it.
    */
  def shared[A: Resource : OptManifest](opener: => A): ManagedResource[A] = {
    @volatile var sharedReference: Option[(Int, A)] = None
    val lock = new AnyRef

    def acquire = {
      lock.synchronized {
        val (referenceCount, sc) = sharedReference match {
          case None =>
            val r = opener
            implicitly[Resource[A]].open(r)
            (1, r)
          case Some((oldReferenceCount, sc)) =>
            (oldReferenceCount + 1, sc)
        }
        sharedReference = Some((referenceCount, sc))
        sc
      }
    }

    val resource = new Resource[A] {

      override def close(r: A): Unit = {
        lock.synchronized {
          sharedReference match {
            case Some((oldReferenceCount, sc)) =>
              if (r != sc) {
                throw new IllegalArgumentException
              }
              if (oldReferenceCount == 1) {
                implicitly[Resource[A]].close(sc)
                sharedReference = None
              } else {
                sharedReference = Some((oldReferenceCount - 1, sc))
              }
            case None =>
              throw new IllegalStateException
          }
        }
      }
    }

    new DefaultManagedResource[A](acquire)(resource, implicitly[OptManifest[A]])
  }

  import scala.language.implicitConversions
  implicit def extractedEitherToEither[A, B](extracted: ExtractedEither[A, B]) : Either[A, B] = extracted.either
}

