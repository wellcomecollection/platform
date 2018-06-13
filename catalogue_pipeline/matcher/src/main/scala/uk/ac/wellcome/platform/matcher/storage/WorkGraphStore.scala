package uk.ac.wellcome.platform.matcher.storage

import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.models.matcher.WorkNode
import uk.ac.wellcome.platform.matcher.lockable._
import uk.ac.wellcome.platform.matcher.models.{WorkGraph, WorkUpdate}
import uk.ac.wellcome.storage.GlobalExecutionContext._
import uk.ac.wellcome.storage.type_classes.IdGetter

import scala.concurrent.Future


class WorkGraphStore @Inject()(
                                workNodeDao: WorkNodeDao,
                                dynamoLockingService: DynamoLockingService
                              ) extends Logging with Locker[DynamoLockingService] {

  override implicit val lockingService = dynamoLockingService

  import Lockable._
  import Locked._

  private def getLockedWorks[T](locks: Either[LockFailures[T], Iterable[Locked[WorkNode]]])(
    implicit lockingService: LockingService,
    idGetter: IdGetter[T]
  ): Iterable[Locked[WorkNode]] = {
    locks match {
      case Right(workNodes) => workNodes.toSet
      case Left(lockFailures) => lockFailures.succeeded.unlock match {
        case Left(unlockFailures) =>
          warn(s"Failed to unlock $unlockFailures after locking")
        case Right(_) =>
          debug(s"Successfully released part of a failed batch lock.")
      }
        Iterable.empty
    }
  }

  private def getUnlockedWorks(unlocks: Either[UnlockFailures[WorkNode], Iterable[WorkNode]]): Iterable[WorkNode] = {
    unlocks match {
      case Right(workNodes) => workNodes.toSet
      case Left(unlockFailures: LockingFailures) => unlockFailures.failed.unlock match {
        case Left(unlockFailures) =>
          warn(s"Failed to unlock $unlockFailures after locking")
        case Right(_) =>
          debug(s"Successfully released part of a failed batch lock.")
      }
    }
    Iterable.empty
  }

  def findAffectedWorks(workUpdate: WorkUpdate): Future[WorkGraph] = Future {

    val directlyAffectedWorkIds = workUpdate.referencedWorkIds + workUpdate.workId
    val directlyAffectedWorksLocks = lockAll[WorkNode](directlyAffectedWorkIds.map(Identifier))(workNodeDao.get(directlyAffectedWorkIds))
    val directlyAffectedWorks = getLockedWorks(directlyAffectedWorksLocks)

    val affectedComponentIds = directlyAffectedWorks.map(_.t.componentId).toSet
    val affectedWorksLocks = workNodeDao.getByComponentIds(affectedComponentIds).lock
    val affectedWorks = getLockedWorks(affectedWorksLocks)

    WorkGraph(affectedWorks.toSet)
  }

  def put(graph: WorkGraph): Future[Set[WorkNode]] = Future {
    graph.nodes.map(lockedNode => workNodeDao.put(lockedNode.t))

    val unlocks = graph.nodes.unlock

    getUnlockedWorks(unlocks).toSet
  }
}
