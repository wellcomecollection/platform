package uk.ac.wellcome.platform.matcher.lockable

import uk.ac.wellcome.storage.type_classes.IdGetter

trait Lockable[T] {
  def lock(t: T): Either[LockFailure, Locked[T]]
  def unlock(t: Locked[T]): Either[UnlockFailure, T]
}

object Lockable {
  def apply[T](implicit lockable: Lockable[T]): Lockable[T] =
    lockable

  implicit class LockableOps[A: Lockable](a: A) {
    def lock = Lockable[A].lock(a)
  }

  implicit def createLockable[T, L <: LockingService](
                                                       implicit
                                                       lockingService: L,
                                                       idGetter: IdGetter[T]
                                                     ): Lockable[T] = new Lockable[T] {
    def lock(t: T): Either[LockFailure, Locked[T]] = {
      val identifier = Identifier(idGetter.id(t))
      val lock = lockingService.lockRow(identifier)

      lock.map(_ => Locked(t))
    }
    def unlock(lockedT: Locked[T]): Either[UnlockFailure, T] = {
      val identifier = Identifier(idGetter.id(lockedT.t))
      val unlock = lockingService.unlockRow(identifier)

      unlock.map(_ => lockedT.t)
    }
  }
}
