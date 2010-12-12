package resource

import java.sql._
import org.junit._
import Assert._

import scala.util.continuations._
/**
 * This class is just to make sure we compile...
 */
object JDBCHelper {

  val initDriver = {
        val name = "org.apache.derby.jdbc.EmbeddedDriver";
        Class.forName(name).newInstance();
        name
   }
  

  //TODO - This is a kludge!
  class ResultSetIterator(resultSet : ResultSet) extends scala.Iterator[ResultSet] {
     /** Does this iterator provide another element?
      */
     def hasNext: Boolean = resultSet.next

     /** Returns the next element of this iterator.
      */
     def next(): ResultSet = resultSet
  }

  
}

class TestJDBC {
  import JDBCHelper._
  
  val tmp = initDriver  //Hack to init the JDBC driver...

  def makeConnection : Connection @suspendable =
      managed(DriverManager.getConnection("jdbc:derby:derbyDB;create=true","","")) !

  @Test
  def testInsertWithStatement() : Unit = {

    reset {
      val conn = makeConnection
      val statement = managed(conn.createStatement) !

      // This would throw an exception if there were some issue...
      statement.executeUpdate("CREATE TABLE josh (fun integer)")

      val ps = managed(conn.prepareStatement("INSERT INTO josh (fun) VALUES (?)")) !
      
      ps.setInt(1, 2)
      ps.addBatch()
      ps.setInt(1, 2)
      ps.addBatch
      assertTrue("Failed to insert second piece of data", ps.executeBatch.forall(_ >= 0))

      val rs = managed(statement.executeQuery("SELECT * from josh")).toTraversable(new ResultSetIterator(_))

      for(result <- rs) {
        assertEquals(2, result.getInt(1))
      }

      // Execute query again, looping through results
      assertEquals("Failed to pull head of result set.",2, rs.head.getInt(1))

      // Execute the query one more time for good measure ;)
      assertEquals(2, rs.size)

      statement.executeUpdate("DROP TABLE josh")
      ()
    }
    ()
  }
}
