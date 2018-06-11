package uk.ac.wellcome.platform.matcher.lockable

import java.time.{Duration, Instant}

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.query.Condition
import com.gu.scanamo.{DynamoFormat, Scanamo, Table}
import com.gu.scanamo.syntax._
import com.gu.scanamo.syntax.{attributeExists, not}
import grizzled.slf4j.Logging
import javax.inject.Inject

class DynamoLockingService @Inject()(dynamoDBClient: AmazonDynamoDB, config: DynamoLockingServiceConfig)
  extends LockingService with Logging {

  implicit val instantLongFormat =
    DynamoFormat.coercedXmap[Instant, Long, IllegalArgumentException](
      Instant.ofEpochSecond
    )(
      _.getEpochSecond
    )

  private val defaultDuration = Duration.ofSeconds(3600)
  private val table = Table[RowLock](config.tableName)

  private def getExpiry = {
    val created = Instant.now()
    val expires = created.plus(defaultDuration)

    (created, expires)
  }


  def lockRow(id: Identifier): Either[LockFailure, RowLock] = {
    val (created, expires) = getExpiry
    val rowLock = RowLock(id.id, created, expires)

    debug(s"Trying to create RowLock: $rowLock")

    val scanamoOps = table
      .given(not(attributeExists('id)) or Condition('expires < rowLock.expires.getEpochSecond))
      .put(rowLock)

    val result = Scanamo.exec(dynamoDBClient)(scanamoOps)

    debug(s"Got $result when creating $rowLock")

    result
      .left.map(e => LockFailure(id, e.toString))
      .right.map(_ => rowLock)
  }

  def unlockRow(id: Identifier): Either[UnlockFailure, Unit] = {

    debug(s"Trying to unlock row: $id")

    val scanamoOps = table
      .given(attributeExists('id) )
      .delete('id -> id.id)

    val result = Scanamo.exec(dynamoDBClient)(scanamoOps)

    debug(s"Got $result when unlocking $id")

    result
      .left.map(e => UnlockFailure(id, e.toString))
      .right.map(_ => ())
  }
}

case class DynamoLockingServiceConfig(tableName: String)