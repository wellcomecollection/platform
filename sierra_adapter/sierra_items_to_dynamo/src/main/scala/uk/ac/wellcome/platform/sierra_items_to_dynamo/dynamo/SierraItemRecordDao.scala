package uk.ac.wellcome.platform.sierra_items_to_dynamo.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{ConditionalCheckFailedException, PutItemResult}
import com.google.inject.Inject
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.Logging
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_items_to_dynamo.sink.SierraItemsDynamoSink.logger
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class SierraItemRecordDao @Inject()(dynamoDbClient: AmazonDynamoDB,
                                    dynamoConfigs: Map[String, DynamoConfig])
    extends Logging {

  private val tableConfigId = "sierraToDynamo"

  private val dynamoConfig = dynamoConfigs.getOrElse(
    tableConfigId,
    throw new RuntimeException(
      s"SierraTransformableDao ($tableConfigId) dynamo config not available!"
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
        .put(sierraItemRecord)) match {
      case Right(_) =>
        debug(s"Successfully saved item ${sierraItemRecord.id} to DynamoDB")
      case Left(error: ConditionalCheckFailedException) =>
        info(
          s"Conditional check failed saving ${sierraItemRecord.id} to DynamoDB")
      case Left(error) =>
        warn(s"Failed saving ${sierraItemRecord.id} to DynamoDB", error)
    }
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
