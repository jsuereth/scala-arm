import java.net.{ServerSocket, Socket}
import java.io._
import resource._

object EchoServer {
  def handleConnection(connection : Socket) {
    val input = ManagedResource(connection.getInputStream).map( is => new BufferedReader(new InputStreamReader(is))).toTraversable( br => new JavaBufferedReaderLineIterator(br))
    val output = ManagedResource(connection.getOutputStream).map( os => new PrintWriter(new BufferedWriter(new OutputStreamWriter(os))))

    for (outStream <- output; line <- input) {
      println("Server returning: " + line)
      outStream.println(line)
      outStream.flush()
    }
  }

  def main(args : Array[String]) : Unit = {
	  val server = new ServerSocket(8007);
    val connection = ManagedResource.make(server)(_.accept)(_.close)

    while(true) {
      for( c <- connection) handleConnection(c)
    }
  }
}

object EchoClient {
  def main(args : Array[String]) : Unit = {
    for {
      connection <- ManagedResource(new Socket("localhost", 8007))
      out <- ManagedResource.make(connection)(_.getOutputStream)(_.close).map(os => new PrintWriter(new BufferedWriter(new OutputStreamWriter(os))))
	    in <- ManagedResource.make(connection)(_.getInputStream)(_.close).map(is => new BufferedReader(new InputStreamReader(is)))
    } {
      out.println("Test Echo Server!")
      out.flush()
	    println("Client Received: " + in.readLine)             
    }
  }
}
