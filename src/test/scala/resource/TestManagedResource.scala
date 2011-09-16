package resource

import _root_.java.{io => jio}

/**
 * This is a basic abstraction for an iterator that fetches new content as needed.
 */
abstract class FetchIterator[T] extends Iterator[T] {
	var fetched = false
	var nextItem : Option[T] = None
	protected def fetchNext(): Option[T]
	override def hasNext = {
		if (!fetched) {
			nextItem = fetchNext()
			fetched = true
		}
		nextItem.isDefined
	}
	override def next() = {
		if (!hasNext) throw new NoSuchElementException("EOF")
		fetched = false
		nextItem.get
	}
}

/** This class creates an iterator from a buffered iterator.  Used to test toTraverable method */
class JavaBufferedReaderLineIterator(br : jio.BufferedReader) extends FetchIterator[String] {
  override def fetchNext() = br.readLine() match {
    case null => None
    case s => Some(s)
  }
}

object FakeResource {
  val GEN_DATA_ERROR =  "Attempted to generate data when resource is not opened!"
  val CLOSE_ERROR = "Attempting to close unopened resource!"
  val OPEN_ERROR = "Attempting to open already opened resource!"
}

/**
 * This is a fake resource type we can utilize.
 */
class FakeResource {
   import java.util.concurrent.atomic.AtomicBoolean
   import FakeResource._
   private val opened = new AtomicBoolean(false)

   def open() : Unit = {
      if(!opened.compareAndSet(false,true)) {
         error(OPEN_ERROR)
      }
   }

   def close() : Unit = {
     if(!opened.compareAndSet(true, false)) {
        error(CLOSE_ERROR)
     }
   }
   protected def makeData = math.random
   def generateData = if(opened.get) makeData else error(GEN_DATA_ERROR)
   def isOpened = opened.get
}

import org.junit._
import Assert._

class TestManagedResource {
  /**
   * This type trait is used to override the default type trait and  give us
   * the use of the managed function for slightly nicer API.   We create this implicit
   * to allow subclasses of FakeResource because otherwise subclasses would *not* use this type trait
   * due to the invariant nature of Resource.
   *
   * TODO - Can we make Resource be contravariant or covariant?
   */
  implicit def fakeResourceTypeTrait[A <: FakeResource] = new Resource[A] {
    override def open(r : A) = r.open()
    override def close(r : A) = r.close()
  }

   @Test
   def mustOpenAndClose() {     
     val r = new FakeResource();
     assertFalse("Failed to begin closed!", r.isOpened)
     val mr = managed(r)
      assertFalse("Creating managed resource opens the resource!", r.isOpened)
      for(r <- mr ) {
          assertTrue("Failed to open resource", r.isOpened)
      }
      assertFalse("Failed to close resource", r.isOpened)
   }  

   @Test
   def mustExtractValue() {
     val r = new FakeResource();
     val mr = managed(r)
      assertFalse("Failed to begin closed!", r.isOpened)
      val monad = for(r <- mr ) yield {
          assertTrue("Failed to open resource", r.isOpened)
          r.generateData
      }      
      val result = monad.opt
      assertTrue("Failed to extract a result", result.isDefined)
      assertFalse("Failed to close resource", r.isOpened)
   }  

   @Test
   def mustNest() {
     val r = new FakeResource();
     val mr = managed(r)
     val r2 = new FakeResource();
     val mr2 = managed(r2)
 
      assertFalse("Failed to begin closed!", r.isOpened)
      assertFalse("Failed to begin closed!", r2.isOpened)
      for(r <- mr; r2 <- mr2 ) {
          assertTrue("Failed to open resource", r.isOpened)
          assertTrue("Failed to open resource", r2.isOpened)
      }      
      assertFalse("Failed to close resource", r.isOpened)
      assertFalse("Failed to close resource", r2.isOpened)
   }  
   

   @Test
   def mustNestForYield() {
     val r = new FakeResource();
     val mr = managed(r)
     val r2 = new FakeResource();
     val mr2 = managed(r2)
     val monad = for { r <- mr
                       r2 <- mr2 
                     } yield r.generateData + r2.generateData      
     //This can't compile as the monad is not an extractable resource!!!
     //assertTrue("Failed to extract a result", monad.opt.isDefined)      
     assertTrue("Failed to extract a result", monad.map(identity[Double]).opt.isDefined)            
     assertFalse("Failed to close resource", r.isOpened)
     assertFalse("Failed to close resource", r2.isOpened)
   }  

   @Test
   def mustSupportValInFor() {
     val r = new FakeResource();
     val mr = managed(r)
     val r2 = new FakeResource();
     val mr2 = managed(r2)
      val monad = for { r <- mr
          val x = r.generateData
          r2 <- mr2 
          val x2 = r2.generateData
      } yield x + x2      
      //This can't compile as the monad is not an extractable resource!!!
      //assertTrue("Failed to extract a result", monad.opt.isDefined)
      assertTrue("Failed to extract a result", monad.map(identity[Double]).opt.isDefined)            
      assertFalse("Failed to close resource", r.isOpened)
      assertFalse("Failed to close resource", r2.isOpened)
   }  



   @Test
   def mustAcquireFor() {
     val r = new FakeResource();
     val mr = managed(r)
      assertFalse("Failed to begin closed!", r.isOpened)
      val result = mr.acquireFor { r =>
          assertTrue("Failed to open resource", r.isOpened)
      }      
      assertFalse("Failed to close resource", r.isOpened)
      assertTrue("Failed to get return value", result.isRight)
   }  

  @Test
   def mustCloseOnException() {
     val r = new FakeResource();
     val mr = managed(r)
      assertFalse("Failed to begin closed!", r.isOpened)
      val result = mr.acquireFor { r =>
          assertTrue("Failed to open resource", r.isOpened)
          error("Some Exception")
      }      
      assertFalse("Failed to close resource", r.isOpened)
      assertTrue("Failed to catch exception", result.isLeft)
   }  

  @Test
  def mustAcquireAndGet() {
   val r = new FakeResource();
   val mr = managed(r)
    assertFalse("Failed to begin closed!", r.isOpened)
    val result = mr.acquireAndGet { r =>
        assertTrue("Failed to open resource", r.isOpened)

        r.generateData
    }
    assertNotNull("Failed to extract a result", result)
    assertFalse("Failed to close resource", r.isOpened)
  }

  @Test
  def mustReturnFirstExceptionInAcquireAndGet() {
    val r = new FakeResource()
    val mr = managed(r)(Resource.reflectiveCloseableResource, implicitly[Manifest[FakeResource]])
    try {
      val result = mr.acquireAndGet { r => r.generateData }
      fail("SHould not make it here, due to previous error!")
    } catch {
      case e =>
          assertEquals("Failed to order exceptions appropriately", FakeResource.GEN_DATA_ERROR, e.getMessage)
    }
  }

  @Test
  def mustJoinSequence() {
    val resources =  (1 to 10).map(i => new FakeResource()).toSeq
    val managedResources = resources.map(managed(_))
    val unified = join(managedResources)

    for(all <- unified) {
      assertTrue("Failed to open resources!", all.forall(_.isOpened))
      all.map(_.generateData).sum      //Make sure no errors are thrown...

      assertTrue("Failed to order properly!", all.zip(resources).forall({ case (x,y) => x == y }))      
    }

     assertFalse("Failed to close resource!", resources.forall(_.isOpened))

  }


  @Test
  def mustCreateTraversable() {
    val resource : ManagedResource[FakeResource] = managed(new FakeResource {
      override protected def makeData = 1.0
    })
    val traversable : Traversable[Double] = resource.map(_.generateData).map(List(_)).toTraversable
    traversable.foreach { x =>
      assertTrue("Failed to traverse correct data!", math.abs(1.0 - x) < 0.0001)
    }
  }

  @Test
  def mustCreateTraversableForExpression() {
    val resource : ManagedResource[FakeResource] = managed(new FakeResource {
      override protected def makeData = 1.0
    })
    val traversable = for( r <- resource ) yield List(r.generateData)
    traversable.toTraversable.foreach { x =>
      assertTrue("Failed to traverse correct data!", math.abs(1.0 - x) < 0.0001)
    }
  }

  @Test
  def mustCreateTraversableMultiLevelForExpression() {
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
  def mustErrorOnTraversal() {
    var caught = false
    try {
      val resource = managed(new FakeResource {
        override protected def makeData = 1.0
      })
      val traversable : Traversable[Any] = resource.map(ignore => (new Traversable[Any] {
        def foreach[U](f : Any => U) : Unit = error("Do not continue!")
      } : Traversable[Any])).toTraversable

      traversable.foreach( x => ())
    } catch {
      case e =>
         caught = true
    }
    assertTrue("Exceptions during traversale are propogated by default!",caught)

  }
  @Test
  def mustAllowApplyUsage() {
    val r = new FakeResource();

    assertFalse("Failed to begin closed!", r.isOpened)
    val result = managed(r) acquireAndGet { r =>
        assertTrue("Failed to open resource", r.isOpened)
        r.generateData
    }
    assertFalse("Failed to close resource", r.isOpened)
  }

  @Test
  def withResourceMustBeAwesome() {
    val outer = new FakeResource
    val inners = List(new FakeResource, new FakeResource, new FakeResource)
    val all = outer :: inners

    val results = withResources {
      val mo = managed(outer).reflect[Double]
      //TODO - Figure out the stink here!
      //for(r <- inners; val mi = managed(r).reflect[List[Double]]) yield mi.generateData + mo.generateData
      mo.generateData
    }
    // If we have no exceptions, we were mostly awesome!

    for( r <- all) {
      assertFalse("Failed to close a resource!", r.isOpened)
    }
  }
}
