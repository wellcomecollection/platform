package uk.ac.wellcome.storage.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{
  ConditionalCheckFailedException,
  ProvisionedThroughputExceededException
}
import com.google.inject.Inject
import com.gu.scanamo.error.{ConditionNotMet, ScanamoError}
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.query.{KeyEquals, UniqueKey}
import com.gu.scanamo.syntax.{attributeExists, not, _}
import com.gu.scanamo.{DynamoFormat, Scanamo, Table}
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.GlobalExecutionContext.context
import uk.ac.wellcome.storage.type_classes.{
  IdGetter,
  VersionGetter,
  VersionUpdater
}

import scala.concurrent.Future

class VersionedDao @Inject()(
  dynamoDbClient: AmazonDynamoDB,
  dynamoConfig: DynamoConfig
) extends Logging {

  private def updateBuilder[T](record: T)(
    implicit evidence: DynamoFormat[T],
    versionUpdater: VersionUpdater[T],
    versionGetter: VersionGetter[T],
    idGetter: IdGetter[T],
    updateExpressionGenerator: UpdateExpressionGenerator[T]
  ): Option[ScanamoOps[Either[ScanamoError, T]]] = {
    val version = versionGetter.version(record)
    val newVersion = version + 1

    val updatedRecord = versionUpdater.updateVersion(record, newVersion)

    updateExpressionGenerator.generateUpdateExpression(updatedRecord).map {
      updateExpression =>
        Table[T](dynamoConfig.table)
          .given(
            not(attributeExists('id)) or
              (attributeExists('id) and 'version < newVersion)
          )
          .update(
            UniqueKey(KeyEquals('id, idGetter.id(record))),
            updateExpression
          )
    }
  }

  def updateRecord[T](record: T)(
    implicit evidence: DynamoFormat[T],
    versionUpdater: VersionUpdater[T],
    idGetter: IdGetter[T],
    versionGetter: VersionGetter[T],
    updateExpressionGenerator: UpdateExpressionGenerator[T]
  ): Future[Unit] =
    Future {
      val id = idGetter.id(record)
      debug(s"Attempting to update Dynamo record: $id")

      updateBuilder(record) match {
        case Some(ops) =>
          Scanamo.exec(dynamoDbClient)(ops) match {
            case Left(ConditionNotMet(e: ConditionalCheckFailedException)) =>
              throw DynamoNonFatalException(e)
            case Left(scanamoError) => {
              val exception = new RuntimeException(scanamoError.toString)
              warn(s"Failed to update Dynamo record: $id", exception)
              throw exception
            }
            case Right(_) => {
              debug(s"Successfully updated Dynamo record: $id")
            }
          }
        case None => ()
      }
    }.recover {
      case t: ProvisionedThroughputExceededException =>
        throw DynamoNonFatalException(t)
    }

  def getRecord[T](id: String)(
    implicit evidence: DynamoFormat[T]): Future[Option[T]] = Future {
    val table = Table[T](dynamoConfig.table)

    debug(s"Attempting to retrieve Dynamo record: $id")
    Scanamo.exec(dynamoDbClient)(table.get('id -> id)) match {
      case Some(Right(record)) => {
        debug(s"Successfully retrieved Dynamo record: $id")

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
        debug(s"No Dynamo record found for id: $id")

        None
      }
    }
  }
}
