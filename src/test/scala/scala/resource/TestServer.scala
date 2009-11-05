import java.net.{ServerSocket, Socket}
import java.io._
import scala.resource._

object EchoServer {
  def main(args : Array[String]) : Unit = {
    import resource._
    val server = new ServerSocket(8007);
    while(true) {
      for { connection <- managed(server.accept)
        outStream <- managed(new PrintWriter(new BufferedWriter(new OutputStreamWriter(connection.getOutputStream))))
        line <- managed(new BufferedReader(new InputStreamReader(connection.getInputStream))).toTraversable( br => new JavaBufferedReaderLineIterator(br))
      } {
        println("Server returning: " + line)
        outStream.println(line)
        outStream.flush()
      }
    }
  }
}

object EchoClient {
  def main(args : Array[String]) : Unit = {
    import resource._
    for { connection <- managed(new Socket("localhost", 8007))
      out <- managed(new PrintWriter(new BufferedWriter(new OutputStreamWriter(connection.getOutputStream))))
      in <- managed(new BufferedReader(new InputStreamReader(connection.getInputStream)))
    } {         
      out.println("Test Echo Server!")
      out.flush()
      println("Client Received: " + in.readLine)             
    }
  }
}
