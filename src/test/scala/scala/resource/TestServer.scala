import java.net.{ServerSocket, Socket}
import java.io._
import resource._

object EchoServer {

   def main(args : Array[String]) : Unit = {
	val server = new ServerSocket(8007);
        while(true) {
           for { connection <- ManagedResource(server.accept)
                 outStream <- ManagedResource(new PrintWriter(new BufferedWriter(new OutputStreamWriter(connection.getOutputStream))))             
                 line <- ManagedResource(new BufferedReader(new InputStreamReader(connection.getInputStream))).toTraversable( br => new JavaBufferedReaderLineIterator(br))
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
      for { connection <- ManagedResource(new Socket("localhost", 8007))
            out <- ManagedResource(new PrintWriter(new BufferedWriter(new OutputStreamWriter(connection.getOutputStream))))
	    in <- ManagedResource(new BufferedReader(new InputStreamReader(connection.getInputStream)))
      } {         
             out.println("Test Echo Server!")
             out.flush()
	     println("Client Received: " + in.readLine)             
      }
   }
}
