package uk.ac.wellcome.platform.matcher.lockable

import uk.ac.wellcome.storage.type_classes.IdGetter

case class Locked[T](t: T)

object Locked {

  implicit class LockedOps[T](a: Locked[T])(implicit lockable: Lockable[T],
                                            idGetter: IdGetter[T]) {
    def unlock: Either[UnlockFailures[T], T] = {
      lockable.unlock(a)
    }
  }

  implicit class LockedSeqOps[T](a: Iterable[Locked[T]])(
    implicit lockable: Lockable[T],
    idGetter: IdGetter[T]) {
    def unlock: Either[UnlockFailures[T], Iterable[T]] = {
      val locked = a.map(Lockable[T].unlock)

      val (leftEither, rightEither) = locked.partition(_.isLeft)
      val (left, right) =
        (leftEither.map(_.left.get), rightEither.map(_.right.get))

      if (left.nonEmpty) {
        val failed =
          left.foldLeft[Iterable[Locked[T]]](Iterable.empty)((acc, o) =>
            acc ++ o.failed)
        val succeeded = right

        Left(UnlockFailures(failed, succeeded))
      } else {
        Right(right)
      }
    }
  }
}
