package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishResult
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.ReplicationRequest

object NextNotifierFlow {

  def apply(snsNextConfig: SNSConfig, snsProgressConfig: SNSConfig)(
    implicit snsClient: AmazonSNS)
    : Flow[ReplicationRequest, PublishResult, NotUsed] = {

    Flow[ReplicationRequest]
      .via(
        SnsPublishFlow[ReplicationRequest](
          snsClient,
          snsNextConfig,
          subject = "archivist_completed"))
      .log("published notification")
  }
}
