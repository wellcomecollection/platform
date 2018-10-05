package uk.ac.wellcome.platform.archive.common.progress.flows

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNS
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressUpdate
}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor

object ProgressUpdateAndPublishFlow {

  def apply(
    snsClient: AmazonSNS,
    snsConfig: SNSConfig,
    progressMonitor: ProgressMonitor
  ) = {

    import Progress._

    val snsPublishFlow =
      SnsPublishFlow[Progress](snsClient, snsConfig, Some("progress-update"))

    val progressUpdateFlow: Flow[ProgressUpdate, Progress, NotUsed] =
      ProgressUpdateFlow(progressMonitor)

    progressUpdateFlow.via(snsPublishFlow)
  }
}
