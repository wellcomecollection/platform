package uk.ac.wellcome.sierra_adapter.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.Logging
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class SierraTransformableDao @Inject()(
  dynamoDbClient: AmazonDynamoDB,
  dynamoConfigs: Map[String, DynamoConfig]
) extends Logging {

  private val tableConfigId = "merger"

  private val dynamoConfig = dynamoConfigs.getOrElse(
    tableConfigId,
    throw new RuntimeException(
      s"SierraTransformableDao ($tableConfigId) dynamo config not available!"
    )
  )

  private val table = Table[SierraTransformable](dynamoConfig.table)

  private def scanamoExec[T](op: ScanamoOps[T]) =
    Scanamo.exec(dynamoDbClient)(op)

  private def putRecord(record: SierraTransformable) = {
    val newVersion = record.version + 1

    table
      .given(
        not(attributeExists('id)) or
          (attributeExists('id) and 'version < newVersion)
      )
      .put(record.copy(version = newVersion))
  }

  def updateRecord(record: SierraTransformable): Future[Unit] = Future {
    debug(s"About to update record $record")
    scanamoExec(putRecord(record)) match {
      case Left(err) =>
        warn(s"Failed updating record ${record.sourceId}", err)
        throw err
      case Right(_) => ()
    }
  }

  def getRecord(id: String): Future[Option[SierraTransformable]] = Future {
    scanamoExec(table.get('id -> id)) match {
      case Some(Right(record)) => Some(record)
      case Some(Left(scanamoError)) =>
        val exception = new RuntimeException(scanamoError.toString)
        error(s"An error occurred while retrieving $id from DynamoDB",
              exception)
        throw exception
      case None => None
    }
  }
}
