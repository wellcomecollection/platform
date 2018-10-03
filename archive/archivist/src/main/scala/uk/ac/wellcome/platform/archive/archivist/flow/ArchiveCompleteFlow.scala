package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.alpakka.sns.scaladsl.SnsPublisher
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNSAsync
import com.amazonaws.services.sns.model.PublishResult
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.models.ArchiveComplete

import scala.util.Success

object ArchiveCompleteFlow {
  def apply(topicArn: String)(implicit snsClient: AmazonSNSAsync)
    : Flow[ArchiveComplete, PublishResult, NotUsed] =
    Flow[ArchiveComplete]
      .map(toJson(_))
      // TODO: Log error here
      .collect { case Success(json) => json }
      .log("notification serialised")
      .via(SnsPublisher.flow(topicArn))
      .log("published notification")
}
