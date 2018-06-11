package uk.ac.wellcome.platform.matcher.lockable

import grizzled.slf4j.Logging

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

    locked.right.map(_ => get) match {
      case Right(r: MaybeGot) => successfulLock(id, r)
      case Left(l: LockFailure) => failedLock(l)
    }
  }

  def lockAll[T, F](ids: Iterable[Identifier])(getAll: => Iterable[Either[F, T]]): Either[LockAllFailure[F], Iterable[Locked[T]]] = {
    val identifierLocks: Either[LockFailures[Identifier], Iterable[Locked[Identifier]]] = ids.lock

    val gotten: Iterable[Either[F, T]] = if(identifierLocks.isRight) {
      getAll
    } else {
      Iterable.empty
    }

    val (leftEither, rightEither) = gotten.partition(_.isLeft)
    val (left, right) =
      (leftEither.map(_.left.get), rightEither.map(_.right.get))

    if(left.isEmpty) {
      Right(right.map(Locked(_)))
    } else {
      Left(LockAllFailure(ids, left))
    }
  }
}

case class LockAllFailure[F](ids: Iterable[Identifier], failures: Iterable[F])
