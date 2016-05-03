package resource
package cats

import _root_.cats._

/** Import this for cats typeclasses associated with ManagedResource. */
object Instances extends ManagedResourceInstances

/** Instances of typeclasses for ManagedResource[T]. */
trait ManagedResourceInstances extends ManagedResourceInstances1 {
	// TODO - just semigroup?
	implicit def managedMonoid[A](implicit m: Monoid[A]): Monoid[ManagedResource[A]] = new Monoid[ManagedResource[A]] {
	    def empty: ManagedResource[A] =
	      constant(m.empty)

	    def combine(x: ManagedResource[A], y: ManagedResource[A]): ManagedResource[A] =
	      (x and y) map { case (l,r) => m.combine(l,r) }
  }
}

trait ManagedResourceInstances1 {
	implicit def managedSemigroup[A](implicit m: Semigroup[A]): Semigroup[ManagedResource[A]] = new Semigroup[ManagedResource[A]] {
	    def combine(x: ManagedResource[A], y: ManagedResource[A]): ManagedResource[A] =
	      (x and y) map { case (l,r) => m.combine(l,r) }
  }

  // Note: We can't safely expose ApplicativeFunctor because
  // `constant` is not guaranteed to be hte right "pure" for all types.
  // If we theorize that all types are in one of two sets:
  // - those with a `Resource[_]` type trait
  // - those which ar constant,
  // Then we could implement (succesffully) an applicative instance.
  // Unfortunately, this scenario is not actually implementable.
  implicit def constFlatmap: FlatMap[ManagedResource] = new FlatMap[ManagedResource] {

  	override def flatMap[A, B](fa: ManagedResource[A])(f: A => ManagedResource[B]): ManagedResource[B] =
  	  fa flatMap f

    override def ap[A, B](f: ManagedResource[A => B])(fa: ManagedResource[A]): ManagedResource[B] =
      (f and fa) map { case (f, a) => f(a) }

    override def map[A, B](fa: ManagedResource[A])(f: A => B): ManagedResource[B] =
      fa map f

    override def product[A, B](fa: ManagedResource[A], fb: ManagedResource[B]): ManagedResource[(A, B)] =
      fa and fb
  }
}