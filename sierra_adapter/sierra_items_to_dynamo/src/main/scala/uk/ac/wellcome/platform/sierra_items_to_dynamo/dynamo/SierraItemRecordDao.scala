package uk.ac.wellcome.platform.sierra_items_to_dynamo.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.Logging
import uk.ac.wellcome.models.SierraItemRecord
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class SierraItemRecordDao @Inject()(dynamoDbClient: AmazonDynamoDB,
                                    dynamoConfigs: Map[String, DynamoConfig])
    extends Logging {

  private val tableConfigId = "sierraToDynamo"

  private val dynamoConfig = dynamoConfigs.getOrElse(
    tableConfigId,
    throw new RuntimeException(
      s"MergedSierraRecordDao ($tableConfigId) dynamo config not available!"
    )
  )

  val table = Table[SierraItemRecord](dynamoConfig.table)

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
    Scanamo.get[SierraItemRecord](dynamoDbClient)(dynamoConfig.table)(
      'id -> id) match {
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
