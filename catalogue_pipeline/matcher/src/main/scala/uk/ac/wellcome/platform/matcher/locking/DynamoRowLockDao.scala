package uk.ac.wellcome.platform.matcher.lockable

import java.time.{Duration, Instant}

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
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

    debug(s"Trying to create RowLock: $rowLock")

    val scanamoOps = table
      .given(
        not(attributeExists('id)) or Condition(
          'expires < rowLock.created.getEpochSecond))
      .put(rowLock)

    val result = Scanamo.exec(dynamoDBClient)(scanamoOps)

    debug(s"Got $result when creating $rowLock")

    result match {
      case Right(_) => rowLock
      case Left(error) =>
        throw new FailedLockException(s"Failed to lock $id", error)
    }
  }

  def unlockRows(contextId: String) = Future {
    debug(s"Trying to unlock context: $contextId")

    val maybeRowLocks: immutable.Seq[Either[DynamoReadError, RowLock]] =
      Scanamo.queryIndex[RowLock](dynamoDBClient)(table.name, index)(
        'contextId -> contextId)

    val rowLockIds = maybeRowLocks.collect {
      case Right(rowLock) => rowLock.id
    }.toSet

    maybeRowLocks.collect {
      case Left(error) => {
        info(s"Error $error when unlocking $contextId")
        throw FailedUnlockException(s"Failed to unlock [$contextId] $error")
      }
    }

    debug(s"Trying to unlock rows: $rowLockIds")
    val scanamoOps = table.deleteAll('id -> rowLockIds)
    Scanamo.exec(dynamoDBClient)(scanamoOps)
    ()
  }
}

case class DynamoLockingServiceConfig(tableName: String, indexName: String)

case class FailedLockException(private val message: String = "",
                               private val cause: Throwable = None.orNull)
    extends Throwable

case class FailedUnlockException(private val message: String = "",
                                 private val cause: Throwable = None.orNull)
    extends Throwable

case class Identifier(id: String)
case class RowLock(id: String,
                   contextId: String,
                   created: Instant,
                   expires: Instant)
