package uk.ac.wellcome.platform.archive.registrar.flows
import akka.stream.alpakka.sns.scaladsl.SnsPublisher
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.sns.AmazonSNSAsync
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.registrar.models.{BagRegistrationCompleteNotification, RegisterRequestContext, StorageManifest}

import scala.util.{Failure, Success}

object SnsPublishFlow extends Logging {

  def apply(snsConfig: SNSConfig)(implicit snsClient:AmazonSNSAsync) =
    Flow[(StorageManifest, RegisterRequestContext)]
      .flatMapConcat({
        case (manifest, context) =>
          Source
            .single((manifest, context))
            .map {
              case (m, c) => BagRegistrationCompleteNotification(c.requestId, m)
            }
            .log("created notification")
            .map(serializeCompletedNotification)
            .log("notification serialised")
            .via(SnsPublisher.flow(snsConfig.topicArn))
            .map((_, context))
      })

  private def serializeCompletedNotification(notification: BagRegistrationCompleteNotification) = {
    toJson(notification) match {
      case Success(json) => json
      case Failure(e)    => throw e
    }
  }
}
