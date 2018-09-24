package uk.ac.wellcome.platform.archive.common.flows

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.{PublishRequest, PublishResult}
import grizzled.slf4j.Logging
import io.circe.Encoder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig

object SnsPublishFlow extends Logging {
  def apply[T](
                snsClient: AmazonSNS,
                snsConfig: SNSConfig,
                subject: String = ""
              )(implicit encode: Encoder[T]): Flow[T, PublishResult, NotUsed] = {

    def publish(t: T) = toJson[T](t)
      .map(new PublishRequest(snsConfig.topicArn, _, subject))
      .map(snsClient.publish)

    ProcessLogDiscardFlow[T, PublishResult]("sns_publish")(publish)
  }
}