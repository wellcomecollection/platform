package uk.ac.wellcome.platform.archive.common.messaging

import akka.NotUsed
import akka.stream.ActorAttributes
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.{PublishRequest, PublishResult}
import grizzled.slf4j.Logging
import io.circe.Encoder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.flows.ProcessLogDiscardFlow

import scala.util.Try

/** Publishes a message to SNS.
  *
  * This flow discards work if there's a failure when publishing to SNS.
  *
  */
object SnsPublishFlow extends Logging {
  def apply[T](
    snsClient: AmazonSNS,
    snsConfig: SNSConfig,
    maybeSubject: Option[String] = None
  )(implicit encode: Encoder[T]): Flow[T, PublishResult, NotUsed] = {

    def publish(t: T) =
      toJson[T](t)
        .map { messageString =>
          debug(s"snsPublishMessage: $messageString")
          maybeSubject match {
            case Some(subject) =>
              new PublishRequest(snsConfig.topicArn, messageString, subject)
            case _ =>
              new PublishRequest(snsConfig.topicArn, messageString)
          }
        }
        .flatMap(publishRequest => Try(snsClient.publish(publishRequest)))
    ProcessLogDiscardFlow[T, PublishResult]("sns_publish")(publish)
      .withAttributes(
        ActorAttributes.dispatcher(
          "akka.stream.materializer.blocking-io-dispatcher"))
  }

  def apply[T](
    snsClient: AmazonSNS,
    snsConfig: SNSConfig,
    subject: String
  )(implicit encode: Encoder[T]): Flow[T, PublishResult, NotUsed] =
    apply(
      snsClient = snsClient,
      snsConfig = snsConfig,
      maybeSubject = Some(subject)
    )
}
