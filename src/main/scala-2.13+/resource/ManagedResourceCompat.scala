package resource

import scala.collection.{ Iterable, IterableOnce }

/**
 * @tparam R the resource type
 */
private[resource] trait ManagedResourceCompat[+R] { self: ManagedResource[R] =>
  /**
   * This method creates a `Iterable` in which all performed methods 
   * are done within the context of an "open" resource.
   * 
   * *Note:* Every iteration will attempt to open and close the resource!
   *
   * @param f  A function that transforms the raw resource into an Iterator of elements of type B.
   *
   * @return A Iterable of elements of type B. 
   */
  def toIterable[B](implicit ev: R <:< IterableOnce[B]): Iterable[B]

}
