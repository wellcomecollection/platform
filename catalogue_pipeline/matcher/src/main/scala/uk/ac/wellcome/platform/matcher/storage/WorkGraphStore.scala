package uk.ac.wellcome.platform.matcher.storage

import com.google.inject.Inject
import com.gu.scanamo.error.DynamoReadError
import com.twitter.inject.Logging
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.matcher.WorkNode
import uk.ac.wellcome.platform.matcher.lockable._
import uk.ac.wellcome.platform.matcher.models.{WorkGraph, WorkUpdate}
import uk.ac.wellcome.storage.GlobalExecutionContext._

import scala.concurrent.Future

class WorkGraphStore @Inject()(
                                workNodeDao: WorkNodeDao,
                                dynamoLockingService: DynamoLockingService
                              ) extends Logging with Locker[DynamoLockingService] {

  implicit val lockingService = dynamoLockingService

  import Lockable._

  private def unlockFailureStrategy[T](lockedList: Iterable[Locked[T]]) = {
    lockedList.unlock match {
      case Left(unlockFailures) =>
        warn(s"Failed to unlock $unlockFailures after locking")
      case Right(_) =>
        debug(s"Successfully released part of a failed batch lock.")
    }

    throw new GracefulFailureException(
      new RuntimeException("Failed to perform batched unlock."))
  }

  private def getLockedWorks[T](locks: Either[LockFailures[T], Iterable[Locked[WorkNode]]]) = {
    locks match {
      case Right(workNodes) => workNodes.toSet
      case Left(lockFailures) =>  unlockFailureStrategy(lockFailures.succeeded)
    }
  }

  private def getUnlockedWorks[T](unlocks: Either[UnlockFailures[WorkNode], Iterable[WorkNode]]) = {
    unlocks match {
      case Right(workNodes) => workNodes.toSet
      case Left(unlockFailures: LockingFailures) => unlockFailureStrategy(unlockFailures.failed)
    }
  }

  def findAffectedWorks(workUpdate: WorkUpdate): Future[WorkGraph] = Future {
    val directlyAffectedWorkIds = workUpdate.referencedWorkIds + workUpdate.workId

    val directlyAffectedWorksLocks = lockAll[WorkNode](directlyAffectedWorkIds.map(Identifier))(workNodeDao.get(directlyAffectedWorkIds))

    val directlyAffectedWorks = getLockedWorks(directlyAffectedWorksLocks)

    val affectedComponentIds = directlyAffectedWorks.map(_.t.componentId)

    val affectedWorksLocks = workNodeDao.getByComponentIds(affectedComponentIds).lock

    val affectedWorks = getLockedWorks(affectedWorksLocks)

    WorkGraph(affectedWorks)
  }

  def put(graph: WorkGraph): Future[Set[WorkNode]] = Future {
    graph.nodes.map(lockedNode => workNodeDao.put(lockedNode.t))

    val unlocks = graph.nodes.unlock

    getUnlockedWorks(unlocks)
  }
}
