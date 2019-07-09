package resource

import scala.collection.{ Iterable, IterableOnce }

private[resource] trait OperationsCompat[+R] {
  res: ManagedResourceOperations[R] =>

  def toIterable[B](implicit ev: R <:< IterableOnce[B]): Iterable[B] =
    new ManagedIterable[B, R] {
      val resource = res

      protected val iterate = ev.apply(_)
    }

}
