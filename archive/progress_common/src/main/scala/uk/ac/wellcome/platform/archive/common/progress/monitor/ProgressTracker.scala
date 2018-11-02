package uk.ac.wellcome.platform.archive.common.progress.monitor

import java.util.UUID

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.google.inject.Inject
import com.gu.scanamo._
import com.gu.scanamo.error.ConditionNotMet
import com.gu.scanamo.syntax._
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.progress.models._
import uk.ac.wellcome.storage.dynamo._

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.util.{Failure, Success, Try}

class ProgressTracker @Inject()(
  dynamoClient: AmazonDynamoDB,
  dynamoConfig: DynamoConfig
)(implicit ex: ExecutionContext) extends Logging {
  import Progress._

  def get(id: UUID): Future[Option[Progress]] = Future {
    blocking(Scanamo.get[Progress](dynamoClient)(dynamoConfig.table)(
      'id -> id
    )) match {
      case Some(Right(progress)) => Some(progress)
      case Some(Left(error)) => {
        val exception = new RuntimeException(
          s"Failed to get progress tracker ${error.toString}")
        warn(s"Failed to get Dynamo record: ${id}", exception)
        throw exception
      }
      case scala.None => scala.None
    }
  }

  def initialise(progress: Progress): Future[Progress] = {
    val progressTable = Table[Progress](dynamoConfig.table)
    debug(s"initializing archive progress tracker with $progress")

    val ops = progressTable
      .given(not(attributeExists('id)))
      .put(progress)

    Future {
      blocking(Scanamo.exec(dynamoClient)(ops)) match {
        case Left(e: ConditionalCheckFailedException) =>
          throw IdConstraintError(
            s"There is already a progress tracker with id:${progress.id}",
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
  }

  def update(update: ProgressUpdate): Try[Progress] = {
    debug(s"Updating Dynamo record ${update.id} with: $update")

    val eventsUpdate = appendAll('events -> update.events.toList)
    val mergedUpdate = update match {
      case _: ProgressEventUpdate =>
        eventsUpdate
      case statusUpdate: ProgressStatusUpdate =>
        val bagUpdate = statusUpdate.affectedBag.map(bag => set(
          'bag -> bag)).toList

        (List(eventsUpdate, set('status -> statusUpdate.status)) ++ bagUpdate).reduce(_ and _)
      case callbackStatusUpdate: ProgressCallbackStatusUpdate =>
        eventsUpdate and set(
          'callback \ 'status -> callbackStatusUpdate.callbackStatus)
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
        debug(
          s"Successfully updated Dynamo record: ${update.id}, got $progress")
        Success(progress)
      }
    }
  }
}

final case class IdConstraintError(
  private val message: String,
  private val cause: Throwable
) extends Exception(message, cause)
