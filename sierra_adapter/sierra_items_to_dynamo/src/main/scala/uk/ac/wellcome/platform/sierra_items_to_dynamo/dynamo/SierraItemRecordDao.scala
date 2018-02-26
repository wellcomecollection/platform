package uk.ac.wellcome.platform.sierra_items_to_dynamo.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.Logging
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class SierraItemRecordDao @Inject()(dynamoDbClient: AmazonDynamoDB,
                                    dynamoConfig: DynamoConfig)
    extends Logging {

  val table = Table[SierraItemRecord](dynamoConfig.table)

  private def scanamoExec[T](op: ScanamoOps[T]) =
    Scanamo.exec(dynamoDbClient)(op)

  private def putRecord(record: SierraItemRecord) = {
    val newVersion = record.version + 1

    table
      .given(
        not(attributeExists('id)) or
          (attributeExists('id) and 'version < newVersion)
      )
      .put(record.copy(version = newVersion))
  }

  def updateItem(record: SierraItemRecord): Future[Unit] = Future {
    debug(s"About to update record $record")
    scanamoExec(putRecord(record)) match {
      case Left(err) =>
        warn(s"Failed updating record ${record.id}", err)
        throw err
      case Right(_) => ()
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
        error(
          s"An error occurred while retrieving item $id: $readError",
          exception)
        throw exception
    }
  }

}
