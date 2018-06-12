package uk.ac.wellcome.platform.matcher.lockable

import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.type_classes.IdGetter

import scala.util.{Failure, Success, Try}

trait Locker[L <: LockingService] extends Logging {

  implicit val lockingService: L

  import Lockable._

  private def lock(identifier: Identifier) =
    lockingService.lockRow(identifier)

  private def successfulLock[T, F](id: Identifier, getResult: Option[Either[F, T]]) =
    getResult match {
      case Some(Right(t)) => Some(Right(Locked(t)))
      case Some(Left(e)) =>
        Some(Left(LockFailure(id, e.toString)))
      case _ => None
    }

  private def failedLock(lockFailure: LockFailure) =
    Some(Left(lockFailure))

  def lock[T, F](id: Identifier)(get: => Option[Either[F, T]]): Option[Either[LockFailure, Locked[T]]] = {

    type MaybeGot = Option[Either[F, T]]

    val locked: Either[LockFailure, RowLock] = lock(id)

    locked.right.map(_ => Try(get)) match {
      case Right(Success(r: MaybeGot)) => successfulLock(id, r)
      case Right(Failure(e)) => failedLock(LockFailure(id, e.toString))
      case Left(l: LockFailure) => failedLock(l)
    }
  }

  def lockAll[T, F](ids: Iterable[Identifier])(getAll: => Iterable[Either[F, T]])(implicit idGetter: IdGetter[T]): Either[LockFailures[Identifier], Iterable[Locked[T]]] = {
    ids.lock.right.flatMap((locks: Iterable[Locked[Identifier]]) => {

      val getAllResult: Iterable[Either[F, T]] = Try(getAll) match {
        case Success(result) => result
        case Failure(e) => {
          error(s"Failed to get $ids after locking!", e)
          Iterable.empty[Either[F, T]]
        }
      }

      val rights = getAllResult
        .filter(_.isRight)
        .map(_.right.get)

      val idsOfT = rights
        .map(idGetter.id)
        .toSet

      val idsOfLocks = locks
        .map(_.t.id)
        .toSet

      if(idsOfT.sameElements(idsOfLocks)) {
        Right(
          rights.map(Locked(_))
        )
      } else {
        Left(
          LockFailures(Iterable.empty[Identifier], locks)
        )
      }
    })

  }
}
