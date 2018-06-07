package uk.ac.wellcome.platform.matcher.lockable

import java.time.Instant

trait LockingService {
  def lockRow(id: Identifier): Either[LockFailure, RowLock]
  def unlockRow(id: Identifier): Either[UnlockFailure, Unit]
}

case class Identifier(value: String)
case class RowLock(
  id: String,
  created: Instant,
  expires: Instant
)