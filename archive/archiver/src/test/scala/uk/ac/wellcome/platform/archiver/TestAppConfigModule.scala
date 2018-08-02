package uk.ac.wellcome.platform.archiver

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archiver.models._

import scala.concurrent.duration._

class TestAppConfigModule(queueUrl: String, bucketName: String) extends AbstractModule {

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
      flushInterval = 20 seconds
    )

    val bagUploaderConfig = BagUploaderConfig(
      uploadNamespace = bucketName
    )

    AppConfig(
      s3ClientConfig,
      bagUploaderConfig,
      cloudwatchClientConfig,
      sqsClientConfig,
      sqsConfig,
      metricsConfig,
    )
  }

  @Provides
  def providesS3ClientConfig(appConfig: AppConfig) =
    appConfig.s3ClientConfig

  @Provides
  def providesCloudwatchClientConfig(appConfig: AppConfig) =
    appConfig.cloudwatchClientConfig

  @Provides
  def providesSQSConfig(appConfig: AppConfig) =
    appConfig.sqsConfig

  @Provides
  def providesSQSClientConfig(appConfig: AppConfig) =
    appConfig.sqsClientConfig

  @Provides
  def providesMetricsConfig(appConfig: AppConfig) =
    appConfig.metricsConfig

  @Provides
  def providesBagUploaderConfig(appConfig: AppConfig) =
    appConfig.bagUploaderConfig
}