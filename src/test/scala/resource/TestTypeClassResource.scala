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

object TypeClassResource {
  val GEN_DATA_ERROR =  "Attempted to generate data when resource is not opened!"
  val CLOSE_ERROR = "Attempting to close unopened resource!"
  val OPEN_ERROR = "Attempting to open already opened resource!"

  implicit object TypeClassResourceHelper extends Resource[TypeClassResource] {
    override def open(r : TypeClassResource) = r.open
    override def close(r : TypeClassResource) = r.close()
  }
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
}
