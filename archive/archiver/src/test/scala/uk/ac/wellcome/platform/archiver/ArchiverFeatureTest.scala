package uk.ac.wellcome.platform.archiver

import com.google.inject.{AbstractModule, Guice, Provides}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archiver.config.{AppConfig, CloudwatchClientConfig, S3ClientConfig, SQSClientConfig}
import uk.ac.wellcome.platform.archiver.modules._

import scala.concurrent.duration._

class ArchiverFeatureTest extends FunSpec
  with Matchers
  with ScalaFutures
  with Messaging
  with AkkaS3 {

  it("fails") {
    withLocalSqsQueueAndDlq(queuePair => {
      sendNotificationToSQS(queuePair.queue, "hello")

      val app = new ArchiverApp {
        override val injector = Guice.createInjector(
          new TestAppConfigModule(queuePair.queue.url),
          AkkaModule,
          AkkaS3ClientModule,
          CloudWatchClientModule,
          SQSClientModule
        )
      }

      app.run()
    })
  }

}

class TestAppConfigModule(queueUrl: String) extends AbstractModule {
  @Provides
  def providesAppConfig = {

    val s3ClientConfig = S3ClientConfig(
      accessKey = Some("accessKey1"),
      secretKey = Some("verySecretKey1"),
      region = "localhost",
      endpoint = Some("localhost:33333")
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

    AppConfig(
      s3ClientConfig,
      cloudwatchClientConfig,
      sqsClientConfig,
      sqsConfig,
      metricsConfig
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
}