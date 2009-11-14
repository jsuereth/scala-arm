package scala.resource

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


class FakeResource {
   import java.util.concurrent.atomic.AtomicBoolean

   private val opened = new AtomicBoolean(false)

   def open() : Unit = {
      if(!opened.compareAndSet(false,true)) {
         error("Attempting to open already opened resource!")
      }
   }

   def close() : Unit = {
     if(!opened.compareAndSet(true, false)) {
        error("Attempting to close unopened resource!")
     }
   }

   def generateData = if(opened.get) Math.random else error("Attempted to generate data when resource is not opened!")
   def isOpened = opened.get
}

class ManagedFakeResource(r : FakeResource) extends AbstractManagedResource[FakeResource,FakeResource] {
        override protected def open = { r.open(); r}
        override protected def translate(handle : FakeResource) = handle
        override protected def unsafeClose(handle : FakeResource) : Unit = handle.close()
        override protected val caughtException = List(classOf[Throwable])
}

import org.junit._
import Assert._
import ManagedResource._

class TestManagedResource {
   @Test
   def mustOpenAndClose() {     
     val r = new FakeResource();
     assertFalse("Failed to begin closed!", r.isOpened)
     val mr = new ManagedFakeResource(r)
      assertFalse("Creating managed resource opens the resource!", r.isOpened)
      for(r <- mr ) {
          assertTrue("Failed to open resource", r.isOpened)
      }
      assertFalse("Failed to close resource", r.isOpened)
   }  

   @Test
   def mustExtractValue() {
     val r = new FakeResource();
     val mr = new ManagedFakeResource(r)
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
     val mr = new ManagedFakeResource(r)
     val r2 = new FakeResource();
     val mr2 = new ManagedFakeResource(r2)
 
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
     val mr = new ManagedFakeResource(r)
     val r2 = new FakeResource();
     val mr2 = new ManagedFakeResource(r2)
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
     val mr = new ManagedFakeResource(r)
     val r2 = new FakeResource();
     val mr2 = new ManagedFakeResource(r2)
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
     val mr = new ManagedFakeResource(r)
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
     val mr = new ManagedFakeResource(r)
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
     val mr = new ManagedFakeResource(r)
      assertFalse("Failed to begin closed!", r.isOpened)
      val result = mr.acquireAndGet { r =>
          assertTrue("Failed to open resource", r.isOpened)

          r.generateData
      }      
      assertTrue("Failed to extract a result", result != null)
      assertFalse("Failed to close resource", r.isOpened)
   }  
}
