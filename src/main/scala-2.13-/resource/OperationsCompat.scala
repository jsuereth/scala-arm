package resource

import scala.collection.{ Traversable, TraversableOnce }

private[resource] trait OperationsCompat[+R] {
  res: ManagedResourceOperations[R] =>

  def toTraversable[B](implicit ev: R <:< TraversableOnce[B]): Traversable[B] =
    new ManagedTraversable[B, R] {
      val resource = res

      protected val traverse = ev.apply(_)
    }

}
