package uk.ac.wellcome.platform.archive.archivist

import akka.Done
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}
import uk.ac.wellcome.platform.archive.archivist.flow._
import uk.ac.wellcome.platform.archive.archivist.models.BagUploaderConfig
import uk.ac.wellcome.platform.archive.common.config.models.Parallelism
import uk.ac.wellcome.platform.archive.common.flows.SupervisedMaterializer
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.typesafe.Runnable

import scala.concurrent.Future

class Archivist(
  messageStream: MessageStream[NotificationMessage, Unit],
  bagUploaderConfig: BagUploaderConfig,
  snsNextConfig: SNSConfig,
  snsProgressConfig: SNSConfig
)(
  implicit val actorSystem: ActorSystem,
  transferManager: TransferManager,
  s3Client: AmazonS3,
  snsClient: AmazonSNS,
) extends Logging
    with Runnable {

  def run(): Future[Done] = {
    implicit val adapter = Logging(actorSystem.eventStream, "custom")
    implicit val parallelism = Parallelism(bagUploaderConfig.parallelism)
    implicit val materializer = SupervisedMaterializer.resumable
    implicit val executionContext = actorSystem.dispatcher

    debug(s"next topic: $snsNextConfig")
    debug(s"progress topic: $snsProgressConfig")

    val notificationMessageFlow = NotificationMessageFlow(snsProgressConfig)
    val zipFileDownloadFlow = ZipFileDownloadFlow(snsProgressConfig)

    val archiveAndNotifyFlow = ArchiveAndNotifyNextFlow(
      bagUploaderConfig,
      snsProgressConfig,
      snsNextConfig
    )

    val workFlow =
      Flow[NotificationMessage]
        .via(notificationMessageFlow)
        .via(zipFileDownloadFlow)
        .via(archiveAndNotifyFlow)

    messageStream.run("archivist", workFlow)
  }
}
