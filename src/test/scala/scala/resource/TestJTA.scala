import javax.transaction.{UserTransaction,Status}
import scala.resource._

/**
 * This class will use the usertransaction object to "join" transactions or start new ones.
 */
class ManagedUserTransaction(val tx : UserTransaction) extends AbstractManagedResource[UserTransaction]() {

  private var wasStarted = false

  protected def open = {
      wasStarted = (tx.getStatus == Status.STATUS_ACTIVE)
      if(!wasStarted) {
         tx.begin()
      }
      tx
  }

  protected def unsafeClose(handle : UserTransaction) : Unit = {
    if(wasStarted) {
      	tx.commit()
    }
  }
}

/** Mini-DSL to open/close transactions for nested calls */
trait JTAHelper {
   implicit def userTransaction : UserTransaction 
   def transactional[A](f : => A)(implicit tx : UserTransaction) : A = (new ManagedUserTransaction(tx)).acquireAndGet(ignore => f)
}
