import java.net.{ServerSocket, Socket}
import java.io._
import scala.resource._

import org.junit._
import Assert._

trait StopableThread {
  @volatile protected var running = true

  var thread : Option[Thread] = None

  def stop() : Unit = {
     running = false
    // TODO (joshuasuereth) - Figure out how to shut this thing down!
     thread.foreach(_.interrupt())
     thread.foreach(_.join()) //TODO - Is this ok...
  }

  def start() : Unit = {
        val t = new Thread(runnable)
        t.start
        thread = Some(t)
  }
  def runnable : Runnable
}


class EchoServer extends StopableThread {

  override val runnable = new Runnable {
    override def run() : Unit = {
      import resource._
      val server = new ServerSocket(8007);
      while(running) {
        for { connection <- managed(server.accept)
          outStream <- managed(new PrintWriter(new BufferedWriter(new OutputStreamWriter(connection.getOutputStream))))
          input <- managed(new BufferedReader(new InputStreamReader(connection.getInputStream)))
          line <- new JavaBufferedReaderLineIterator(input)
        } {
          println("Server returning: " + line)
          outStream.println(line)
          outStream.flush()
        }
      }
    }
  }
}


class EchoServer2 extends StopableThread {

  import scala.util.continuations._
  def each_line_from(r : BufferedReader) : String @suspendable = shift {
    k =>
      var line = r.readLine
      while(line != null) {
        k(line)
        line = r.readLine
      }
  }

  override val runnable = new Runnable {
    override def run() : Unit = {
      import resource._
      val server = new ServerSocket(8007)
      while(running) {
        reset {
          val connection = managed(server.accept) !
          val output = managed(connection.getOutputStream) !
          val input = managed(connection.getInputStream) !
          val writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)))
          val reader = new BufferedReader(new InputStreamReader(input))
          writer.println(each_line_from(reader))
          writer.flush()
        }
      }

    }
  }
}

class EchoClient {
  def sendAndCheckString(arg : String) : Boolean = { 
    import resource._

    val result = for { connection <- managed(new Socket("localhost", 8007))
      out <- managed(new PrintWriter(new BufferedWriter(new OutputStreamWriter(connection.getOutputStream))))
      in <- managed(new BufferedReader(new InputStreamReader(connection.getInputStream)))
    } yield {         
      out.println(arg)
      out.flush()
      arg == in.readLine
    }
    result.map(x => x).opt.getOrElse(false)
  }
}



class TestSocketServer {
  @Test
  def dummy() {}
  //@Test
  def testSocket() {
     val server = new EchoServer2
     val client = new EchoClient
     server.start
     Thread.sleep(500)
     //TODO - Accept possibility of this timing out.  Use a future.
     val result = client.sendAndCheckString("Hello, World!")
     server.stop()
     assertTrue("Socket Server Failed to respond correctly", result)
  }
}
