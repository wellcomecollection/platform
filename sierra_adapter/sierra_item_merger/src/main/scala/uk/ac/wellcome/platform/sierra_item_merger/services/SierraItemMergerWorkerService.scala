package uk.ac.wellcome.platform.sierra_item_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.storage.{ObjectLocation, ObjectStore}
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.vhs.HybridRecord

import scala.concurrent.{ExecutionContext, Future}

class SierraItemMergerWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  sierraItemMergerUpdaterService: SierraItemMergerUpdaterService,
  objectStore: ObjectStore[SierraItemRecord],
  s3Config: S3Config
)(implicit ec: ExecutionContext) {

  sqsStream.foreach(this.getClass.getSimpleName, process)

  private def process(message: NotificationMessage): Future[Unit] =
    for {
      hybridRecord <- Future.fromTry(fromJson[HybridRecord](message.Message))
      objectLocation = ObjectLocation(
        namespace = s3Config.bucketName,
        key = hybridRecord.s3key)
      record <- objectStore.get(objectLocation)
      _ <- sierraItemMergerUpdaterService.update(record)
    } yield ()

  def stop() = system.terminate()
}
