package uk.ac.wellcome.platform.matcher.lockable

import java.time.{Duration, Instant}

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query.Condition
import com.gu.scanamo.syntax.{attributeExists, not, _}
import com.gu.scanamo.{DynamoFormat, Scanamo, Table}
import grizzled.slf4j.Logging
import javax.inject.Inject

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class DynamoRowLockDao @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  config: DynamoLockingServiceConfig)(implicit context: ExecutionContext)
    extends Logging {

  implicit val instantLongFormat: AnyRef with DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, Long, IllegalArgumentException](
      Instant.ofEpochSecond
    )(
      _.getEpochSecond
    )

  private val defaultDuration = Duration.ofSeconds(3600)
  private val table = Table[RowLock](config.tableName)
  private val index = config.indexName

  private def getExpiry = {
    val created = Instant.now()
    val expires = created.plus(defaultDuration)

    (created, expires)
  }

  def lockRow(id: Identifier, contextId: String) = Future {
    val (created, expires) = getExpiry
    val rowLock = RowLock(id.id, contextId, created, expires)
    debug(s"Locking $rowLock")

    val scanamoOps = table
      .given(
        not(attributeExists('id)) or Condition(
          'expires < rowLock.created.getEpochSecond))
      .put(rowLock)
    val result = Scanamo.exec(dynamoDBClient)(scanamoOps)
    debug(s"Got $result for $rowLock")

    result match {
      case Right(_) => rowLock
      case Left(error) => {
        debug(s"Failed to lock $rowLock $error")
        throw FailedLockException(s"Failed to lock $id", error)
      }
    }
  }.recover {
    case exception: Exception =>
      val errorMsg = s"Problem locking row ${id} in context [$contextId], ${exception.getClass.getSimpleName} ${exception.getMessage}"
      debug(errorMsg)
      throw FailedLockException(errorMsg, exception)
  }

  def unlockRows(contextId: String): Future[Unit] = Future {
    debug(s"Trying to unlock context: $contextId")
    val maybeRowLocks: immutable.Seq[Either[DynamoReadError, RowLock]] =
      Scanamo.queryIndex[RowLock](dynamoDBClient)(table.name, index)(
        'contextId -> contextId)

    debug("maybeRowLocks: " + maybeRowLocks)
    val rowLockIds = maybeRowLocks.collect {
      case Right(rowLock) => rowLock.id
      case Left(error) =>
        info(s"Error $error when unlocking $contextId")
        throw FailedUnlockException(s"Failed to unlock [$contextId] $error", new RuntimeException)
    }.toSet

    debug(s"Unlocking rows: $rowLockIds")
    val deleteAllResults =
      Scanamo.deleteAll(dynamoDBClient)(table.name)('id -> rowLockIds)
    deleteAllResults.foreach { result: BatchWriteItemResult =>
      if (result.getUnprocessedItems.size() > 0) {
        val errorMsg =
          s"Batch delete failed to delete ${result.getUnprocessedItems}"
        debug(errorMsg)
        throw new RuntimeException(errorMsg)
      }
    }
    ()
  }.recover {
    case exception: Exception =>
      val errorMsg = s"Problem unlocking rows in context [$contextId], ${exception.getClass.getSimpleName} ${exception.getMessage}"
      debug(errorMsg)
      throw FailedUnlockException(errorMsg, exception)
  }

}

case class DynamoLockingServiceConfig(tableName: String, indexName: String)

case class FailedLockException(message: String,
                               cause: Throwable)
    extends Exception(message, cause)

case class FailedUnlockException(message: String,
                                 cause: Throwable)
  extends Exception(message, cause)

case class Identifier(id: String)
case class RowLock(id: String,
                   contextId: String,
                   created: Instant,
                   expires: Instant)
