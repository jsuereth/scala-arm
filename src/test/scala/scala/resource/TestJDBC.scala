package scala.resource

/**
 * Created by IntelliJ IDEA.
 * User: josh
 * Date: Nov 2, 2009
 * Time: 7:53:02 PM
 * To change this template use File | Settings | File Templates.
 */


object TestJDBC {
  import java.sql._
  import resource._

  //TODO - This is a kludge!
  class ResultSetIterator(resultSet : ResultSet) extends scala.Iterator[ResultSet] {
     /** Does this iterator provide another element?
      */
     def hasNext: Boolean = resultSet.next

     /** Returns the next element of this iterator.
      */
     def next(): ResultSet = resultSet
  }

  def test() : scala.collection.Traversable[Int] = {

    val connFactory : ManagedResource[Connection] = managed(DriverManager.getConnection("","",""))

    for {
      connection <- connFactory
      stmt <- managed(connection.prepareStatement("select * from foo where id = ?"))
      resultSet <- managed { stmt.setLong(1,2); stmt.getResultSet() }
      row <- new ResultSetIterator(resultSet)
    } {
      println(row.getInt(1))
    }


    val results = connFactory.flatMap( c => managed(c.prepareStatement("foo!"))).flatMap(s => managed(s.getResultSet)).toTraversable(r => new ResultSetIterator(r))
    val ids = for { row <- results } yield row.getInt(1)            

    ids
  }

}