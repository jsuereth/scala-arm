package resource

/** Support for jta transactions. */
package object jta {
  /** Support for using jta transactions as resources.
   * To use in code, simply write: `import resource.jta.transactionSupport`
   */
  implicit def transactionSupport[A <: javax.transaction.Transaction]: Resource[A] = 
    new Resource[A] {
      override def close(r: A) = r.commit()
      override def closeAfterException(r: A, t: Throwable) = r.rollback()
    }
}
