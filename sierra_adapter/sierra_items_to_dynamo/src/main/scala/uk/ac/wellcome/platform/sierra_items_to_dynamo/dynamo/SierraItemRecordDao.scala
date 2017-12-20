package uk.ac.wellcome.platform.sierra_items_to_dynamo.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.Logging
import uk.ac.wellcome.models.SierraItemRecord
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class SierraItemRecordDao(dynamoDbClient: AmazonDynamoDB, tableName: String)
    extends Logging {

  val table = Table[SierraItemRecord](tableName)

  def updateItem(sierraItemRecord: SierraItemRecord): Future[Unit] = Future {
    Scanamo.exec(dynamoDbClient)(
      table
        .given(
          not(attributeExists('id)) or
            (attributeExists('id) and 'modifiedDate < sierraItemRecord.modifiedDate.getEpochSecond)
        )
        .put(sierraItemRecord))
  }

  def getItem(id: String): Future[Option[SierraItemRecord]] = Future {
    Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> id) match {
      case Some(Right(item)) => Some(item)
      case None => None
      case Some(Left(readError)) =>
        val exception = new RuntimeException(
          s"An error occurred while retrieving item $id: $readError")
        error(s"An error occurred while retrieving item $id: $readError",
              exception)
        throw exception
    }
  }

}
