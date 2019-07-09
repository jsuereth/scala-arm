import java.net.{ServerSocket, Socket}
import java.io._

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
  def checkNormalARM(): Unit = {
    socketTestHelper(new EchoServer)
  }

  def socketTestHelper(server : Thread): Unit = {
     val client = new EchoClient
     server.start
     Thread.sleep(500)
     //TODO - Accept possibility of this timing out.  Use a future.
     val result = client.sendAndCheckString("Hello, World!")
     server.join()
     assertTrue("Socket Server Failed to respond correctly", result)
  }
}
