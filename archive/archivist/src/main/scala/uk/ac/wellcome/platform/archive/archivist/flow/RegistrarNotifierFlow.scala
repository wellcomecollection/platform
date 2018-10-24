package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishResult
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.ArchiveComplete
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.progress.models._

object RegistrarNotifierFlow {

  def apply(snsRegistrarConfig: SNSConfig, snsProgressConfig: SNSConfig)(
    implicit snsClient: AmazonSNS)
    : Flow[ArchiveComplete, PublishResult, NotUsed] = {

    Flow[ArchiveComplete]
      .flatMapConcat(
        archiveComplete =>
          Source
            .single(toProgressUpdate(archiveComplete))
            .log("sending bag resource update to progress monitor")
            .via(
              SnsPublishFlow[ProgressUpdate](
                snsClient,
                snsProgressConfig,
                Some("archivist_progress")))
            .map(_ => archiveComplete))
      .via(
        SnsPublishFlow[ArchiveComplete](
          snsClient,
          snsRegistrarConfig,
          Some("archive_completed")))
      .log("published notification")
  }

  def toProgressUpdate(
    archiveComplete: ArchiveComplete): ProgressResourceUpdate = {
    ProgressResourceUpdate(
      archiveComplete.archiveRequestId,
      List(Resource(ResourceIdentifier(archiveComplete.bagId.toString))),
      List(ProgressEvent("bag resource id updated")))
  }
}
