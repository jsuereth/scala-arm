import java.net.{ServerSocket, Socket}
import java.io._
import resource._

import org.junit._
import Assert._

// Echoes what is sent to it once, then dies.
class EchoServer extends Thread {

  override def run() : Unit = {
    import resource._
    for {
      server <- managed(new ServerSocket(8007))
      connection <- managed(server.accept)
      outStream <- managed(new PrintWriter(new BufferedWriter(new OutputStreamWriter(connection.getOutputStream))))
      input <- managed(new BufferedReader(new InputStreamReader(connection.getInputStream)))
      line <- new JavaBufferedReaderLineIterator(input)
    } {
      outStream.println(line)
      outStream.flush()
    }
  }
}

// Another server that echoes what is sent to it on a socket and then dies.
class EchoServerCPS extends Thread {

  import scala.util.continuations._
  def each_line_from(r : BufferedReader) : String @suspendable = shift {
    k =>
      var line = r.readLine
      while(line != null) {
        k(line)
        line = r.readLine
      }
  }

  override def run() : Unit = {
    reset {
      val server = managed(new ServerSocket(8007)) !
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
  def checkNormalARM() {
    socketTestHelper(new EchoServer)
  }
  @Test
  def checkCPSARM() {
    socketTestHelper(new EchoServerCPS)
  }
  def socketTestHelper(server : Thread) {
     val client = new EchoClient
     server.start
     Thread.sleep(500)
     //TODO - Accept possibility of this timing out.  Use a future.
     val result = client.sendAndCheckString("Hello, World!")
     server.join()
     assertTrue("Socket Server Failed to respond correctly", result)
  }
}
