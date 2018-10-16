package uk.ac.wellcome.platform.archive.common.flows
import java.util.UUID

import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNS
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.progress.models.progress.{
  Progress,
  ProgressEvent,
  ProgressStatusUpdate,
  ProgressUpdate
}

object NotifyFailureFlow {
  def apply[T](subject: String, snsConfig: SNSConfig)(toRequestId: T => UUID)(
    implicit snsClient: AmazonSNS) =
    Flow[ArchiveError[T]]
      .map(
        error =>
          ProgressStatusUpdate(
            toRequestId(error.t),
            Progress.Failed,
            List(ProgressEvent(error.toString))
        )
      )
      .via(SnsPublishFlow[ProgressUpdate](snsClient, snsConfig, Some(subject)))
}
