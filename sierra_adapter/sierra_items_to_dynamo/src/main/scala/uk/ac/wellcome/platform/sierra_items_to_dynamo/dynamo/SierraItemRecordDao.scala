package uk.ac.wellcome.platform.sierra_items_to_dynamo.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{ConditionalCheckFailedException, PutItemResult}
import com.google.inject.Inject
import com.gu.scanamo.ops.ScanamoOps
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

  private def scanamoExec[T](op: ScanamoOps[T]) =
    Scanamo.exec(dynamoDbClient)(op)

  private def putRecord(record: SierraItemRecord) = {
    val newVersion = record.version + 1
    val modifiedDate = record.modifiedDate.getEpochSecond

    table
      .given(
        not(attributeExists('id)) or
          (attributeExists('id) and 'version < newVersion) and
          (attributeExists('id) and 'modifiedDate < modifiedDate)
      )
      .put(record.copy(version = newVersion))
  }

  def updateItem(record: SierraItemRecord): Future[Unit] = Future {
    debug(s"About to update record $record")
    scanamoExec(putRecord(record)) match {
      case Left(error: ConditionalCheckFailedException) =>
        info(
          s"Conditional check failed saving ${record.id} to DynamoDB")
      case Left(error) =>
        warn(s"Failed saving ${record.id} to DynamoDB", error)
      case Right(_) => debug(s"Successfully saved item ${record.id} to DynamoDB")
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
