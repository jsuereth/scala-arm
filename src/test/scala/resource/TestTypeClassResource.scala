package resource



/**
 * This is a fake resource type we can utilize.
 */
class TypeClassResource {
   import java.util.concurrent.atomic.AtomicBoolean
   import TypeClassResource._
   private val opened = new AtomicBoolean(false)

   def open() : Unit = {
      if(!opened.compareAndSet(false,true)) {
        sys.error(OPEN_ERROR)
      }
   }

   def close() : Unit = {
     if(!opened.compareAndSet(true, false)) {
       sys.error(CLOSE_ERROR)
     }
   }
   protected def makeData = math.random
   def generateData = if(opened.get) makeData else sys.error(GEN_DATA_ERROR)
   def isOpened = opened.get
}

object TypeClassResource {
  val GEN_DATA_ERROR =  "Attempted to generate data when resource is not opened!"
  val CLOSE_ERROR = "Attempting to close unopened resource!"
  val OPEN_ERROR = "Attempting to open already opened resource!"

  implicit object TypeClassResourceHelper extends Resource[TypeClassResource] {
    override def open(r : TypeClassResource) = r.open
    override def close(r : TypeClassResource) = r.close()
  }
}

class UnitResourceMock extends Resource[Unit] {
  var closed = false
  var closedAfterException = false
  override def close(r: Unit) = closed = true
  override def closeAfterException(r: Unit, t: Throwable) = closedAfterException = true
}

import org.junit._
import Assert._

class TestTypeClassResource {
  @Test
  def mustOpenAndClose() {
    val r = new TypeClassResource();
    assertFalse("Failed to begin closed!", r.isOpened)
    val mr = managed(r)
    assertFalse("Creating managed resource opens the resource!", r.isOpened)
    for(r <- mr ) {
      assertTrue("Failed to open resource", r.isOpened)
    }
    assertFalse("Failed to close resource", r.isOpened)
  }

  @Test
  def mustCloseAfterExcetpion() = {
    implicit val resource1 = new UnitResourceMock
    implicit val resource2 = new UnitResourceMock
    val r = for {
      a <- managed(())(resource1, implicitly)
      b <- managed(())(resource2, implicitly)
    } yield sys.error("error")
    r.acquireFor(_ => ())
    assertFalse(resource1.closed)
    assertTrue(resource1.closedAfterException)
    assertFalse(resource2.closed)
    assertTrue(resource2.closedAfterException)
  }
}
