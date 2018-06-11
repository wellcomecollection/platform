package uk.ac.wellcome.platform.matcher.lockable

sealed trait LockingFailures

case class LockFailures[T](
  failed: Iterable[T],
  succeeded: Iterable[Locked[T]]
) extends LockingFailures

case class UnlockFailures[T](
  failed: Iterable[Locked[T]],
  succeeded: Iterable[T]
) extends LockingFailures