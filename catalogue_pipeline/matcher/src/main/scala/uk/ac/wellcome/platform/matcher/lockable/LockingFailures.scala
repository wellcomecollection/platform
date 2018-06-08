package uk.ac.wellcome.platform.matcher.lockable

sealed trait LockingFailures

case class LockFailures[T](
  failed: IndexedSeq[T],
  succeeded: IndexedSeq[Locked[T]]
) extends LockingFailures

case class UnlockFailures[T](
  failed: IndexedSeq[Locked[T]],
  succeeded: IndexedSeq[T]
) extends LockingFailures