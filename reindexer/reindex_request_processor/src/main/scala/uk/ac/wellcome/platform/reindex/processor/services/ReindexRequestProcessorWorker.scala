package uk.ac.wellcome.platform.reindex.processor.services

import akka.actor.{ActorSystem, Terminated}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.reindexer.{ReindexRequest, ReindexableRecord}
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.{ExecutionContextExecutor, Future}

class ReindexRequestProcessorWorker @Inject()(
  dynamoDbClient: AmazonDynamoDB,
  sqsStream: SQSStream[NotificationMessage],
  system: ActorSystem)
    extends Logging {

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      reindexRequest <- Future.fromTry(
        fromJson[ReindexRequest](message.Message))
      versionedDao = createVersionedDaoFor(reindexRequest)
      maybeReindexableRecord <- versionedDao.getRecord[ReindexableRecord](
        reindexRequest.id)
      _ <- updateRecord(versionedDao, reindexRequest, maybeReindexableRecord)
    } yield ()

  private def updateRecord(
    versionedDao: VersionedDao,
    reindexRequest: ReindexRequest,
    maybeReindexableRecord: Option[ReindexableRecord]) = {
    maybeReindexableRecord match {
      case Some(existingRecord) =>
        if (reindexRequest.desiredVersion > existingRecord.reindexVersion) {
          val mergedRecord =
            existingRecord.copy(reindexVersion = reindexRequest.desiredVersion)
          versionedDao.updateRecord(mergedRecord)
        } else {
          Future.successful(())
        }
      case None =>
        throw new RuntimeException(
          s"VersionedDao has no record for $reindexRequest")
    }
  }

  private def createVersionedDaoFor(
    reindexRequest: ReindexRequest): VersionedDao =
    new VersionedDao(
      dynamoDbClient = dynamoDbClient,
      dynamoConfig = DynamoConfig(
        table = reindexRequest.tableName,
        maybeIndex = None
      )
    )

  def stop(): Future[Terminated] = system.terminate()
}
