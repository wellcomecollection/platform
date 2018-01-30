package uk.ac.wellcome.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.syntax.{attributeExists, not, _}
import com.gu.scanamo.{DynamoFormat, Scanamo, Table}
import com.twitter.inject.Logging
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{VersionUpdater, Versioned}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class VersionedDao @Inject()(
  dynamoDbClient: AmazonDynamoDB,
  dynamoConfig: DynamoConfig
) extends Logging {

  private def putRecord[T<: Versioned](record: T)(implicit evidence: DynamoFormat[T], versionUpdater: VersionUpdater[T]) = {
    val newVersion = record.version + 1

    Table[T](dynamoConfig.table)
      .given(
        not(attributeExists('sourceId)) or
          (attributeExists('sourceId) and 'version < newVersion)
      )
      .put(versionUpdater.updateVersion(record, newVersion))
  }

  def updateRecord[T<: Versioned](record: T)(implicit evidence: DynamoFormat[T], versionUpdater: VersionUpdater[T]): Future[Unit] = Future {
    debug(s"About to update record $record")
    Scanamo.exec(dynamoDbClient)(putRecord(record)) match {
      case Left(err) =>
        warn(s"Failed updating record ${record.sourceId}", err)
        throw err
      case Right(_) => ()
    }
  }

  def getRecord[T<: Versioned](id: String)(implicit evidence: DynamoFormat[T]): Future[Option[T]] = Future {
    val table = Table[T](dynamoConfig.table)
    Scanamo.exec(dynamoDbClient)(table.get('sourceId -> id)) match {
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
