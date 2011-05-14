import resource._

/**
 * Package related methods for maanged resources.
 */
package object resource {

  /** The type used to hold errors and results in the same return value. */
  type ErrorHolder[A] = Either[List[Throwable],A]
  /**
   * Creates a ManagedResource for any type with a Resource type class implementation.   This includes all
   * java.io.Closeable subclasses, and any types that have a close or dispose method.  You can also provide your own
   * resource type class implementations in your own scope.
   */
  def managed[A : Resource : Manifest](opener : => A) : ManagedResource[A] = new DefaultManagedResource(opener)

  /**
   * Constructs a managed resource using function objects for each abstract method.
   *
   * @param opener  The by-name parameter that will open the resource.
   * @param closer  A closure that will close the resource.
   * @param exceptions  A list of exception classes that cannot be ignored to close a resource.
   */
  def makeManagedResource[R : Manifest](opener :  => R)(closer : R => Unit)(exceptions : List[Class[_<:Throwable]]) = {
    implicit val typeTrait = new Resource[R] {
      override def close(r : R) = closer(r)
      override val fatalExceptions = exceptions
    }
    new DefaultManagedResource(opener)
  }

  /** @see CanSafelyTranslate.extractUnManaged */
  def extractUnManaged[T] = CanSafelyMap.extractUnManaged[T]
  /** @see CanSafelyTranslate.extractOption */
  def extractOption[T] = CanSafelyMap.extractOption[T]


  import scala.util.continuations._

  /**
   * Starts  block that will use continuation-based resource handling.   This is where you can nest resources directly
   * without having to worry about closing the block.   Example:
   * 
   */
  def withResources[C](f : => C @cps[Either[List[Throwable],C]]) = reset { Right(f) }

  /**
   * Combined two resources such that they are both opened/closed together.   The first resource is opened before
   * the second resource and closed after the second resource, however the resulting ManagedResource acts like
   * both are opened/closed together.
   * @return A ManagedResource of a tuple containing the initial two resources.
   */
  def and[A,B](r1 : ManagedResource[A], r2 : ManagedResource[B]) = new ManagedResource[(A,B)] with ManagedResourceOperations[(A,B)] {

    override def acquireFor[C](f : ((A,B)) => C) = withResources {
      val resource1 = r1.reflect[C]
      val resource2 = r2.reflect[C]
      f((resource1, resource2))
    }
  }

  /*def joinResources[A](resources : Seq[ManagedResource[A]]) : ManagedResource[Seq[A]] = new ManagedResource[Seq[A]] with ManagedResourceOperations[Seq[A]] {
    def acquireFor[C](f : Seq[A] => C) = withResources {
      val rs = resources.map(_.reflect[C])
      f(rs)
    }
  } */

  /**
   * Takes a sequence of ManagedResource objects and traits them as a ManagedResource of a Sequence of Objects.
   *
   * This is useful for dealing with many resources within the same scope.
   *
   * @param resource   A collection of ManageResources of the same type
   * @return  A ManagedResoruce of a collection of types
   */
  def join[A, MR , CC ](resources : CC)(implicit ev0 : CC <:< Seq[ MR ], ev1 : MR <:< ManagedResource[A]) : ManagedResource[Seq[A]] = {
    //TODO - Use foldLeft
    //TODO - Don't use such a sucky algorithm...
    //We currently assume 1 resource
    //TODO - See if we can provide Hlist implementation as well...
    val itr = (resources.reverseIterator : Iterator[MR])
    val first : ManagedResource[A] = itr.next
    var toReturn : ManagedResource[Seq[A]] = first.map( x => Seq(x))
    while(itr.hasNext) {
      val r1 = toReturn
      val r2 : ManagedResource[A] = itr.next
      toReturn = new ManagedResource[Seq[A]] with ManagedResourceOperations[Seq[A]] {
        override def acquireFor[B](f : Seq[A] => B) : Either[List[Throwable], B] = r1.acquireFor {
            r1seq =>
               r2.acquireAndGet { r2item =>
                  f( r2item :: r1seq.toList)
               }
        }
      }
    }
    toReturn
  }
}

