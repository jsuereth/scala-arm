package resource

import org.junit.{ Assert, Test }, Assert._

trait TestManagedResourceCompat { test: TestManagedResource =>
  @Test
  def mustCreateIterable(): Unit = {
    val resource: ManagedResource[FakeResource] = managed(new FakeResource {
      override protected def makeData = 1.0
    })

    val iterable: Iterable[Double] =
      resource.map(_.generateData).map(List(_)).toIterable

    iterable.foreach { x =>
      assertTrue("Failed to traverse correct data!", math.abs(1.0 - x) < 0.0001)
    }

    iterable.iterator.foreach { x =>
      assertTrue("Failed to traverse correct data!", math.abs(1.0 - x) < 0.0001)
    }
  }

  @Test
  def mustCreateIterableForExpression(): Unit = {
    val resource : ManagedResource[FakeResource] = managed(new FakeResource {
      override protected def makeData = 1.0
    })
    val iterable = resource.map(r => List(r.generateData))
    val result = iterable.toIterable

    result.foreach { x =>
      assertTrue("Failed to traverse correct data!", math.abs(1.0 - x) < 0.0001)
    }

    result.iterator.foreach { x =>
      assertTrue("Failed to traverse correct data!", math.abs(1.0 - x) < 0.0001)
    }
  }

  @Test
  def mustCreateIterableMultiLevelForExpression(): Unit = {
    def resource : ManagedResource[FakeResource] = managed(new FakeResource {
      override protected def makeData = 1.0
    })

    val iterable = for {
      r1 <- resource
      r2 <- resource
      r3 <- resource
    } yield List(r1.generateData, r2.generateData, r3.generateData)

    val result = iterable.toIterable

    result.foreach { x =>
      assertTrue("Failed to traverse correct data!", math.abs(1.0 - x) < 0.0001)
    }

    result.iterator.foreach { x =>
      assertTrue("Failed to traverse correct data!", math.abs(1.0 - x) < 0.0001)
    }
  }

  @Test
  def mustErrorOnTraversal(): Unit = {
    val resource = managed(new FakeResource {
      override protected def makeData = 1.0
    })

    val iterable: Iterable[Any] = resource.map(ignore => (new Iterable[Any] {
      override def foreach[U](f : Any => U) : Unit =
        sys.error("Do not continue!")

      def iterator: Iterator[Any] = new Iterator[Any] {
        def hasNext = true

        def next: Any = sys.error("Do not continue!")
      }
    } : Iterable[Any])).toIterable

    // ---

    var caught = false

    try {
      iterable.foreach { _ => () }
    } catch {
      case e: Throwable =>
        caught = true
    }

    assertTrue("Exceptions during iterable are propogated by default!",caught)

    // ---

    caught = false

    try {
      iterable.iterator.foreach { _ => () }
    } catch {
      case e: Throwable =>
        caught = true
    }

    assertTrue("Exceptions during iterator are propogated by default!",caught)
  }

  protected def rightMap[L, R1, R2](e: Either[L, R1])(f: R1 => R2): Either[L, R2] = e.map(f)
}
