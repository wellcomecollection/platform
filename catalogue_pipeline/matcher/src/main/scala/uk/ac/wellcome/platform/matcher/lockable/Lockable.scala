package uk.ac.wellcome.platform.matcher.lockable

import uk.ac.wellcome.storage.type_classes.IdGetter

trait Lockable[T] {
  def lock(t: T): Either[LockFailures[T], Locked[T]]
  def unlock(t: Locked[T]): Either[UnlockFailures[T], T]
}

object Lockable {
  def apply[T](implicit lockable: Lockable[T]): Lockable[T] =
    lockable

  implicit class LockableOps[A: Lockable](a: A)(implicit idGetter: IdGetter[A]) {
    def lock = Lockable[A].lock(a)
  }

  implicit class LockableListOps[A: Lockable](a: Iterable[A])(
    implicit idGetter: IdGetter[A]) {
    def lock: Either[LockFailures[A], Iterable[Locked[A]]] = {
      val locked = a.map(Lockable[A].lock)

      val (leftEither, rightEither) = locked.partition(_.isLeft)
      val (left, right: Iterable[Locked[A]]) =
        (leftEither.map(_.left.get), rightEither.map(_.right.get))

      if(left.nonEmpty) {
        val failed = left.foldLeft[Iterable[A]](Iterable.empty)(
          (acc, o) => acc ++ o.failed)

        val succeeded = right

        Left(LockFailures(failed, succeeded))
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

    def lock(t: T): Either[LockFailures[T], Locked[T]] = {
      val identifier = Identifier(idGetter.id(t))
      val lock: Either[LockFailure, RowLock] = lockingService.lockRow(identifier)

      lock
        .left.map(_ => LockFailures(Iterable(t), Iterable.empty))
        .right.map(_ => Locked(t))
    }

    def unlock(lockedT: Locked[T]): Either[UnlockFailures[T], T] = {
      val identifier = Identifier(idGetter.id(lockedT.t))
      val unlock = lockingService.unlockRow(identifier)

      unlock
        .left.map(_ => UnlockFailures(Iterable(lockedT), Iterable.empty))
        .right.map(_ => lockedT.t)
    }
  }
}
