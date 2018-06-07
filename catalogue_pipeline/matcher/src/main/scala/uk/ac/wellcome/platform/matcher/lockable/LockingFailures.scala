package uk.ac.wellcome.platform.matcher.lockable

sealed trait LockingFailures

case class LockFailure(value: String) extends LockingFailures
case class UnlockFailure(value: String) extends LockingFailures
