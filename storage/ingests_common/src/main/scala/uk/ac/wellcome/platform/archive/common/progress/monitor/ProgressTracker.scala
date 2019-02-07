package uk.ac.wellcome.platform.archive.common.progress.monitor

import java.util.UUID

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.gu.scanamo._
import com.gu.scanamo.error.ConditionNotMet
import com.gu.scanamo.syntax._
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.bagit.BagId
import uk.ac.wellcome.platform.archive.common.progress.models._
import uk.ac.wellcome.storage.dynamo._

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.util.{Failure, Success, Try}

class ProgressTracker(
  dynamoDbClient: AmazonDynamoDB,
  dynamoConfig: DynamoConfig
)(implicit ec: ExecutionContext) extends Logging {

  val versionedDao = new VersionedDao(
    dynamoDbClient = dynamoDbClient,
    dynamoConfig = dynamoConfig
  )

  def get(id: UUID): Future[Option[Progress]] = versionedDao.getRecord[Progress](id.toString)

  def initialise(progress: Progress): Future[Progress] = {
    val progressTable = Table[Progress](dynamoConfig.table)
    debug(s"initializing archive progress tracker with $progress")

    val ops = progressTable
      .given(not(attributeExists('id)))
      .put(progress)

    Future {
      blocking(Scanamo.exec(dynamoDbClient)(ops)) match {
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
    debug(s"Updating record:${update.id} with:$update")

    val eventsUpdate = appendAll('events -> update.events.toList)
    val mergedUpdate = update match {
      case _: ProgressEventUpdate =>
        eventsUpdate
      case statusUpdate: ProgressStatusUpdate =>
        val bagUpdate = statusUpdate.affectedBag.map(bag =>
          set('bag -> bag) and set('bagIdIndex -> bag.toString)).toList

        (List(eventsUpdate, set('status -> statusUpdate.status)) ++ bagUpdate).reduce(_ and _)
      case callbackStatusUpdate: ProgressCallbackStatusUpdate =>
        eventsUpdate and set(
          'callback \ 'status -> callbackStatusUpdate.callbackStatus)
    }

    val progressTable = Table[Progress](dynamoConfig.table)
    val ops = progressTable
      .given(attributeExists('id))
      .update('id -> update.id, mergedUpdate)

    Scanamo.exec(dynamoDbClient)(ops) match {
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
      case Right(progress) => {
        debug(
          s"Successfully updated Dynamo record: ${update.id}, got $progress")
        Success(progress)
      }
    }
  }

  /** Find stored progress given a bagId, uses the secondary index to link a bag to the ingest(s)
    * that created it.
    *
    * This is intended to meet a particular use case for DLCS during migration and not as part of the
    * public/documented API.  Consider either removing this functionality or enhancing it to be fully
    * featured if a use case arises after migration.
    *
    * return a list of Either BagIngest or error querying DynamoDb
    *
    * Returns at most 30 associated ingests with most recent first -- to simplify the code by avoiding
    * pagination, but still fulfilling DLCS's requirements.
    */
  def findByBagId(bagId: BagId)= {
    val query = Table[BagIngest](dynamoConfig.table)
      .index(dynamoConfig.index)
      .limit(30)
      .query(('bagIdIndex -> bagId.toString).descending)
    Scanamo.exec(dynamoDbClient)(query)
  }
}

final case class IdConstraintError(
  private val message: String,
  private val cause: Throwable
) extends Exception(message, cause)
