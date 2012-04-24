package resource

/**
 * This is a type trait for types that are considered 'resources'.   These types must be opened (potentially) and
 * closed at some point.
 */
trait Resource[R] {
  /**
   * Opens a resource for manipulation.  Note:  If the resource is already open by definition of existence, then
   * this method should perform a no-op (the default implementation).
   */
  def open(r: R): Unit = ()

  /**
   *  Closes a resource.  This method is allowed to throw exceptions.
   */
  def close(r: R): Unit
  
  /**
   * This is called if the resource should be closed *after* an exception was thrown.  The
   * default implementation is to call close itself.
   */
  def closeAfterException(r: R, t: Throwable): Unit = close(r)

  /**
   * Returns the possible exceptions that a resource could throw.   This list is used to catch only relevant
   * exceptions in ARM blocks.  This defaults to be any Exception (but not runtime exceptions, which are
   * assumed to be fatal. 
   */
  def fatalExceptions: Seq[Class[_]] = List(classOf[java.lang.VirtualMachineError],
                                            classOf[java.lang.InterruptedException],
                                            classOf[scala.util.control.ControlThrowable])
}

/**
 * Trait holding type class implementations for Resource.  These implicits will be looked up last in the
 * line, so they can be easily overriden.
 */
sealed trait LowPriorityResourceImplicits {
  /** Structural type for disposable resources */
  type ReflectiveCloseable = { def close() }
  /**
   * This is the type class implementation for reflectively assuming a class with a close method is
   * a resource.
   */
  implicit def reflectiveCloseableResource[A <: ReflectiveCloseable] = new Resource[A] {
    override def close(r: A) = r.close()
    override def toString = "Resource[{ def close() : Unit }]"
  }
  /** Structural type for disposable resources */
  type ReflectiveDisposable = { def dispose() }
  /**
   * This is the type class implementation for reflectively assuming a class with a dispose method is
   * a resource.
   */
  implicit def reflectiveDisposableResource[A <: ReflectiveDisposable] = new Resource[A] {
    override def close(r: A) = r.dispose()
    override def toString = "Resource[{ def dispose() : Unit }]"
  }
}

sealed trait MediumPriorityResourceImplicits extends LowPriorityResourceImplicits {
  import _root_.java.io.Closeable
  import _root_.java.io.IOException
  implicit def closeableResource[A <: Closeable] = new Resource[A] {
    override def close(r: A) = r.close()
    // TODO - Should we actually catch less?   What if there is a user exception not under IOException during
    // processing of a resource.   We should still close it.
    //override val possibleExceptions = List(classOf[IOException])
    override def toString = "Resource[java.io.Closeable]"
  }
  
  //Add All JDBC related handlers.
  implicit def connectionResource[A <: java.sql.Connection] = new Resource[A] {
    override def close(r: A) = r.close()
    override def toString = "Resource[java.sql.Connection]"
  }
  // This will work for Statements, PreparedStatements and CallableStatements.
  implicit def statementResource[A <: java.sql.Statement] = new Resource[A] {
    override def close(r: A) = r.close()
    override def toString = "Resource[java.sql.Statement]"
  }
  // Also handles RowSet
  implicit def resultSetResource[A <: java.sql.ResultSet] = new Resource[A] {
    override def close(r: A) = r.close()
    override def toString = "Resource[java.sql.ResultSet]"
  }
  implicit def pooledConnectionResource[A <: javax.sql.PooledConnection] = new Resource[A] {
    override def close(r: A) = r.close()
    override def toString = "Resource[javax.sql.PooledConnection]"
  }
  // This will ensure
  implicit def rollbackTransactionResource[A <: javax.transaction.Transaction] = new Resource[A] {
    override def close(r: A) = r.commit()
    override def closeAfterException(r: A, t: Throwable) = r.rollback()
  }
}

/**
 * Companion object to the Resource type trait.   This contains all the default implicits in appropriate priority order.
 */
object Resource extends MediumPriorityResourceImplicits
