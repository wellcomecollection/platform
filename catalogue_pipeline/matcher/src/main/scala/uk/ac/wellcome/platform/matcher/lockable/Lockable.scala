package uk.ac.wellcome.platform.matcher.lockable

import uk.ac.wellcome.storage.type_classes.IdGetter

trait Lockable[T] {
  def lock(t: T): Either[LockFailure, Locked[T]]
  def unlock(t: Locked[T]): Either[UnlockFailure, T]
}

object Lockable {
  def apply[T](implicit lockable: Lockable[T]): Lockable[T] =
    lockable

  implicit class LockableOps[A: Lockable](a: A)(implicit idGetter: IdGetter[A]) {
    def lock = Lockable[A].lock(a)
  }

  implicit class LockableListOps[A: Lockable](a: IndexedSeq[A])(
    implicit idGetter: IdGetter[A]) {
    def lock: Either[LockFailure, IndexedSeq[Locked[A]]] = {
      val locked = a.map(Lockable[A].lock)

      val (leftEither, rightEither) = locked.partition(_.isLeft)
      val (left, right) =
        (leftEither.map(_.left.get), rightEither.map(_.right.get))

      if(left.nonEmpty) {
        val unlocks = right.map(_.unlock)

        val (unlockLeftEither, unlockRightEither) = unlocks.partition(_.isLeft)

        val (unlockLeft, unlockRight) =
          (unlockLeftEither.map(_.left.get), unlockRightEither.map(_.right.get))

        // If we get here both our locking operation
        // and out attempt to back it out have failed
        // time to give up!
        if(unlockLeft.nonEmpty) {
          throw new RuntimeException("Error attmepting to unlock contended lock!")
        }

        Left(LockFailure("Could not lock sequence!"))
      } else {
        Right(right)
      }
    }
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
