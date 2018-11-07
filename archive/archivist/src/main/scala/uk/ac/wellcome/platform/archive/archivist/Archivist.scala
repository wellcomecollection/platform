package uk.ac.wellcome.platform.archive.archivist

import akka.Done
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.scaladsl.Flow
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.archivist.flow._
import uk.ac.wellcome.platform.archive.archivist.models.BagUploaderConfig
import uk.ac.wellcome.platform.archive.common.flows.FoldEitherFlow
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.models.{IngestBagRequest, NotificationMessage}

import scala.concurrent.Future

class Archivist(
  s3Client: AmazonS3,
  snsClient: AmazonSNS,
  messageStream: MessageStream[NotificationMessage, Unit],
  bagUploaderConfig: BagUploaderConfig,
  snsRegistrarConfig: SNSConfig,
  snsProgressConfig: SNSConfig
)(implicit val actorSystem: ActorSystem) extends Logging {
  def run(): Future[Done] = {
    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")

    val decider: Supervision.Decider = {
      e => {
        error("Stream failure", e)
        Supervision.Resume
      }
    }

    implicit val materializer: ActorMaterializer = ActorMaterializer(
      ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider)
    )

    debug(s"registrar topic: $snsRegistrarConfig")
    debug(s"progress topic: $snsProgressConfig")

    val workFlow =
      Flow[NotificationMessage]
        .log("notification message")
        .via(
          NotificationMessageFlow(
            parallelism = bagUploaderConfig.parallelism,
            snsClient = snsClient,
            progressSnsConfig = snsProgressConfig
          )
        )
        .log("download zip")
        .via(
          ZipFileDownloadFlow(bagUploaderConfig.parallelism, snsProgressConfig))
        .log("archiving zip")
        .via(
          FoldEitherFlow[
            ArchiveError[IngestBagRequest],
            ZipFileDownloadComplete,
            Unit
            ](ifLeft = Flow[ArchiveError[IngestBagRequest]].map(_ => ()))(
            ifRight = ArchiveAndNotifyRegistrarFlow(
              bagUploaderConfig,
              snsProgressConfig,
              snsRegistrarConfig)))

    messageStream.run("archivist", workFlow)
  }
}
