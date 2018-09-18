package uk.ac.wellcome.platform.archive.archivist.streams.flow

import akka.stream.alpakka.sns.scaladsl.SnsPublisher
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNSAsync
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.models.{ArchiveCompleteNotification, BagLocation, IngestRequestContext}

import scala.util.Success

object ArchiveCompleteFlow {
  def apply(topicArn: String)(implicit snsClient: AmazonSNSAsync) =
    Flow[(BagLocation, IngestRequestContext)]
      .map { case (loc, ctx) => ArchiveCompleteNotification(loc, ctx) }
      .log("created notification")
      .map(toJson(_))
      // TODO: Log error here
      .collect { case Success(json) => json }
      .log("notification serialised")
      .via(SnsPublisher.flow(topicArn))
      .log("published notification")
}
