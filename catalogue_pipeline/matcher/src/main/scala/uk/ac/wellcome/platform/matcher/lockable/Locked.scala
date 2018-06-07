package uk.ac.wellcome.platform.matcher.lockable

case class Locked[T](t: T) {
  def unlock(implicit lockable: Lockable[T]): Either[UnlockFailure, T]= {
    lockable.unlock(this)
  }
}