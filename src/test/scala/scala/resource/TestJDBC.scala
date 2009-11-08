package scala.resource

import java.sql._
import resource._
import org.junit._
import Assert._
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

  @Test
  def test() : Unit = {

    val connFactory : ManagedResource[Connection] = ManagedResource(DriverManager.getConnection("jdbc:derby:derbyDB;create=true","",""))
    val results = connFactory.flatMap( c => ManagedResource(c.prepareStatement("foo!"))).flatMap(s => ManagedResource(s.getResultSet)).toTraversable(r => new ResultSetIterator(r))
    val ids = for { row <- results } yield row.getInt(1)            

    ()
  }
}
