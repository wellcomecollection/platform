package uk.ac.wellcome.platform.archive.notifier.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.notifier.models.NotifierConfig
import uk.ac.wellcome.platform.archive.common.modules._

import scala.concurrent.duration._

class TestAppConfigModule(queue: Queue, topic: Topic) extends AbstractModule {
  @Provides
  def providesAppConfig: NotifierConfig = {
    val cloudwatchClientConfig = CloudwatchClientConfig(
      region = "localhost",
      endpoint = Some("localhost")
    )

    val sqsClientConfig = SQSClientConfig(
      accessKey = Some("access"),
      secretKey = Some("secret"),
      region = "localhost",
      endpoint = Some("http://localhost:9324")
    )
    val sqsConfig = SQSConfig(queue.url)

    val snsClientConfig = SnsClientConfig(
      accessKey = Some("access"),
      secretKey = Some("secret"),
      region = "localhost",
      endpoint = Some("http://localhost:9292")
    )
    val snsConfig = SNSConfig(topic.arn)

    val metricsConfig = MetricsConfig(
      namespace = "namespace",
      flushInterval = 60 seconds
    )

    NotifierConfig(
      cloudwatchClientConfig = cloudwatchClientConfig,
      sqsClientConfig = sqsClientConfig,
      sqsConfig = sqsConfig,
      snsClientConfig = snsClientConfig,
      snsConfig = snsConfig,
      metricsConfig = metricsConfig
    )
  }
}
