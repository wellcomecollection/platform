package uk.ac.wellcome.platform.archive.registrar.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.modules.{
  CloudwatchClientConfig,
  S3ClientConfig,
  SQSClientConfig,
  SnsClientConfig
}
import uk.ac.wellcome.platform.archive.registrar.models.RegistrarConfig

import scala.concurrent.duration._

class TestAppConfigModule(queueUrl: String,
                          bucketName: String,
                          topicArn: String)
    extends AbstractModule {
  @Provides
  def providesAppConfig = {
    val s3ClientConfig = S3ClientConfig(
      accessKey = Some("accessKey1"),
      secretKey = Some("verySecretKey1"),
      region = "localhost",
      endpoint = Some("http://localhost:33333")
    )
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
    val sqsConfig = SQSConfig(queueUrl)
    val metricsConfig = MetricsConfig(
      namespace = "namespace",
      flushInterval = 60 seconds
    )
    val snsClientConfig = SnsClientConfig(
      accessKey = Some("access"),
      secretKey = Some("secret"),
      region = "localhost",
      endpoint = Some("http://localhost:9292")
    )
    val snsConfig = SNSConfig(topicArn)

    RegistrarConfig(
      s3ClientConfig,
      cloudwatchClientConfig,
      sqsClientConfig,
      sqsConfig,
      snsClientConfig,
      snsConfig,
      metricsConfig
    )
  }
}
