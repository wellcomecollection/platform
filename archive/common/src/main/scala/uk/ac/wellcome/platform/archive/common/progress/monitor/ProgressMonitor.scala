package uk.ac.wellcome.platform.archive.common.progress.monitor

import java.time.Instant
import java.time.format.DateTimeFormatter

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.google.inject.Inject
import com.gu.scanamo._
import com.gu.scanamo.error.ConditionNotMet
import com.gu.scanamo.syntax._
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressUpdate
}
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.util.{Failure, Success, Try}

class ProgressMonitor @Inject()(
  dynamoClient: AmazonDynamoDB,
  dynamoConfig: DynamoConfig
) extends Logging {

  implicit val instantLongFormat: AnyRef with DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, String, IllegalArgumentException](str =>
      Instant.from(DateTimeFormatter.ISO_INSTANT.parse(str)))(
      DateTimeFormatter.ISO_INSTANT.format(_)
    )

  def get(id: String) = {
    Scanamo.get[Progress](dynamoClient)(dynamoConfig.table)(
      'id -> id
    ) match {
      case Some(Right(progress)) => Some(progress)
      case Some(Left(error)) => {
        val exception = new RuntimeException(
          s"Failed to get progress monitor ${error.toString}")
        warn(s"Failed to get Dynamo record: ${id}", exception)
        throw exception
      }
      case None => None
    }
  }

  def create(progress: Progress) = {
    val progressTable = Table[Progress](dynamoConfig.table)
    debug(s"initializing archiveProgressMonitor with $progress")

    val ops = progressTable
      .given(not(attributeExists('id)))
      .put(progress)

    Scanamo.exec(dynamoClient)(ops) match {
      case Left(e: ConditionalCheckFailedException) =>
        throw IdConstraintError(
          s"There is already a monitor with id:${progress.id}",
          e)
      case Left(scanamoError) =>
        val exception = new RuntimeException(
          s"Failed to create progress ${scanamoError.toString}")
        warn(s"Failed to update Dynamo record: ${progress.id}", exception)
        throw exception
      case Right(a) =>
        debug(s"Successfully updated Dynamo record: ${progress.id} $a")
    }
    progress
  }

  def update(update: ProgressUpdate): Try[Progress] = {
    val event = update.event

    val mergedUpdate = update.status match {
      case Progress.None =>
        append('events -> event)
      case status =>
        append('events -> event) and set('result -> status)
    }

    val progressTable = Table[Progress](dynamoConfig.table)
    val ops = progressTable
      .given(attributeExists('id))
      .update('id -> update.id, mergedUpdate)

    Scanamo.exec(dynamoClient)(ops) match {
      case Left(ConditionNotMet(e: ConditionalCheckFailedException)) => {
        val idConstraintError =
          IdConstraintError(s"Progress does not exist for id:${update.id}", e)

        Failure(idConstraintError)
      }

      case Left(scanamoError) => {
        val exception = new RuntimeException(scanamoError.toString)
        warn(s"Failed to update Dynamo record: ${update.id}", exception)

        Failure(exception)
      }

      case r @ Right(progress) => {
        debug(s"Successfully updated Dynamo record: ${update.id}")

        Success(progress)
      }
    }
  }
}

final case class IdConstraintError(
  private val message: String,
  private val cause: Throwable
) extends Exception(message, cause)
