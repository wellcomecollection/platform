package uk.ac.wellcome.platform.matcher.lockable

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo._
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import grizzled.slf4j.Logging

class LockingDao[T](dynamoDbClient: AmazonDynamoDB, config: LockingDaoConfig)(
  implicit
    lockingService: DynamoLockingService,
//    idGetter: IdGetter[T],
    evidence: DynamoFormat[T]
) extends Logging {

  val table = Table[T](config.tableName)

  private def lock(identifier: Identifier) =
    lockingService.lockRow(identifier)

  private def get(identifier: Identifier) =
    Scanamo.exec(dynamoDbClient)(table.get('id -> identifier.value))


  def lockAndGet(identifier: Identifier): Option[Either[LockingDaoFailure, Locked[T]]] = {
    val locked: Either[LockFailure, RowLock] = lock(identifier)

    val foo: Either[LockFailure, Option[Either[DynamoReadError, T]]] = locked.right.map(_ => get(identifier))

    foo match {
      case Right(Some(Right(t))) => Some(Right(Locked(t)))
      case Right(Some(Left(e: DynamoReadError))) => Some(Left(LockingDaoFailure(e.toString)))
      case Right(None) => None
      case _ => Some(Left(LockingDaoFailure("")))
    }
  }
}

case class LockingDaoFailure(value: String)
case class LockingDaoConfig(tableName: String)