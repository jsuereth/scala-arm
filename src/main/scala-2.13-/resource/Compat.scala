package resource

private[resource] object Compat {
  def toRightOption[L, R](e: Either[L, R]): Option[R] = e.right.toOption
}
