package resource

import org.junit.{ Assert, Test }, Assert._

trait TestManagedResourceCompat { test: TestManagedResource =>
  @Test
  def mustCreateTraversable(): Unit = {
    val resource : ManagedResource[FakeResource] = managed(new FakeResource {
      override protected def makeData = 1.0
    })
    val traversable : Traversable[Double] = resource.map(_.generateData).map(List(_)).toTraversable
    traversable.foreach { x =>
      assertTrue("Failed to traverse correct data!", math.abs(1.0 - x) < 0.0001)
    }
  }

  @Test
  def mustCreateTraversableForExpression(): Unit = {
    val resource : ManagedResource[FakeResource] = managed(new FakeResource {
      override protected def makeData = 1.0
    })
    val traversable = for( r <- resource ) yield List(r.generateData)
    traversable.toTraversable.foreach { x =>
      assertTrue("Failed to traverse correct data!", math.abs(1.0 - x) < 0.0001)
    }
  }

  @Test
  def mustCreateTraversableMultiLevelForExpression(): Unit = {
    def resource : ManagedResource[FakeResource] = managed(new FakeResource {
      override protected def makeData = 1.0
    })
    val traversable = for {
      r1 <- resource
      r2 <- resource
      r3 <- resource
    } yield List(r1.generateData, r2.generateData, r3.generateData)
    traversable.toTraversable.foreach { x =>
      assertTrue("Failed to traverse correct data!", math.abs(1.0 - x) < 0.0001)
    }
  }

  @Test
  def mustErrorOnTraversal(): Unit = {
    var caught = false

    try {
      val resource = managed(new FakeResource {
        override protected def makeData = 1.0
      })
      val traversable : Traversable[Any] = resource.map(_ => (new Traversable[Any] {
        def foreach[U](f : Any => U) : Unit = sys.error("Do not continue!")
      } : Traversable[Any])).toTraversable

      traversable.foreach { _ => () }
    } catch {
      case _: Throwable =>
         caught = true
    }

    assertTrue("Exceptions during traversale are propogated by default!",caught)
  }

  protected def rightMap[L, R1, R2](e: Either[L, R1])(f: R1 => R2): Either[L, R2] = e.right.map(f)
}
