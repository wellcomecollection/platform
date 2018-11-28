package uk.ac.wellcome.platform.matcher.locking

import java.util.UUID.randomUUID

import grizzled.slf4j.Logging
import uk.ac.wellcome.monitoring.MetricsSender

import scala.collection.Set
import scala.concurrent.{ExecutionContext, Future}

class DynamoLockingService(
  dynamoRowLockDao: DynamoRowLockDao,
  metricsSender: MetricsSender)(implicit ec: ExecutionContext)
    extends Logging {

  private val failedLockMetricName: String = "WorkMatcher_FailedLock"
  private val failedUnlockMetricName: String = "WorkMatcher_FailedUnlock"

  def withLocks[T](ids: Set[String])(callback: => Future[T]): Future[T] = {
    if (ids.isEmpty) {
      callback
    } else {
      executeWithLocks(ids, callback, randomUUID.toString, ids.map(Identifier))
    }
  }

  private def executeWithLocks[T](ids: Set[String],
                                  f: => Future[T],
                                  contextGuid: String,
                                  identifiers: Set[Identifier]) = {
    debug(s"Locking ids $ids in context $contextGuid")
    val eventuallyExecutedWithLock = for {
      _ <- Future.sequence(
        identifiers.map(dynamoRowLockDao.lockRow(_, contextGuid)))
      result <- f
    } yield {
      result
    }
    eventuallyExecutedWithLock
      .transformWith { triedResult =>
        dynamoRowLockDao
          .unlockRows(contextGuid)
          .flatMap(_ => {
            trace(s"Released locked identifiers $identifiers in $contextGuid")
            Future.fromTry(triedResult)
          })
      }
      .recover {
        case failedLockException: FailedLockException =>
          debug(failedLockErrorMessage("lock", failedLockException))
          metricsSender.incrementCount(failedLockMetricName)
          throw failedLockException
        case failedUnlockException: FailedUnlockException =>
          debug(failedLockErrorMessage("unlock", failedUnlockException))
          metricsSender.incrementCount(failedUnlockMetricName)
          throw failedUnlockException
      }
  }

  private def failedLockErrorMessage[T](failure: String,
                                        exception: Exception) = {
    s"Failed to $failure ${exception.getClass.getSimpleName} ${exception.getMessage}"
  }
}
