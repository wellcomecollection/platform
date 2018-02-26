package uk.ac.wellcome.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.syntax.{attributeExists, not, _}
import com.gu.scanamo.{DynamoFormat, Scanamo, Table}
import com.twitter.inject.Logging
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{Id, VersionUpdater, Versioned}

import uk.ac.wellcome.utils.GlobalExecutionContext.context
import scala.concurrent.Future


class VersionedDao @Inject()(
  dynamoDbClient: AmazonDynamoDB,
  dynamoConfig: DynamoConfig
) extends Logging {

  private def putRecord[T <: Versioned with Id](record: T)(
    implicit evidence: DynamoFormat[T],
    versionUpdater: VersionUpdater[T]) = {
    val newVersion = record.version + 1

    Table[T](dynamoConfig.table)
      .given(
        not(attributeExists('id)) or
          (attributeExists('id) and 'version < newVersion)
      )
      .put(versionUpdater.updateVersion(record, newVersion))
  }

  def updateRecord[T <: Versioned with Id](record: T)(
    implicit evidence: DynamoFormat[T],
    versionUpdater: VersionUpdater[T]): Future[Unit] = Future {
    info(s"Attempting to update Dynamo record: ${record.id}")

    Scanamo.exec(dynamoDbClient)(putRecord(record)) match {
      case Left(err) =>
        warn(s"Failed to updating Dynamo record: ${record.id}", err)

        throw err
      case Right(_) => {
        info(s"Successfully updated Dynamo record: ${record.id}")
      }
    }
  }

  def getRecord[T <: Versioned with Id](id: String)(
    implicit evidence: DynamoFormat[T]): Future[Option[T]] = Future {
    val table = Table[T](dynamoConfig.table)

    info(s"Attempting to retrieve Dynamo record: $id")
    Scanamo.exec(dynamoDbClient)(table.get('id -> id)) match {
      case Some(Right(record)) => {
        info(s"Successfully retrieved Dynamo record: ${record.id}")

        Some(record)
      }
      case Some(Left(scanamoError)) =>
        val exception = new RuntimeException(scanamoError.toString)

        error(
          s"An error occurred while retrieving $id from DynamoDB",
          exception
        )

        throw exception
      case None => {
        info(s"No Dynamo record found for id: $id")

        None
      }
    }
  }
}
