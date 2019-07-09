package resource

import scala.collection.{ Traversable, TraversableOnce }

/**
 * @tparam R the resource type
 */
private[resource] trait ManagedResourceCompat[+R] { self: ManagedResource[R] =>
  /**
   * This method creates a `Traversable` in which all performed methods 
   * are done within the context of an "open" resource.
   * 
   * *Note:* Every iteration will attempt to open and close the resource!
   *
   * @param f  A function that transforms the raw resource into an Iterator of elements of type B.
   *
   * @return A Traversable of elements of type B. 
   */
  def toTraversable[B](implicit ev: R <:< TraversableOnce[B]): Traversable[B]

}
