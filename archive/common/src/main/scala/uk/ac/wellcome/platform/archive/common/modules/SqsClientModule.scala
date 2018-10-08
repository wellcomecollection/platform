package uk.ac.wellcome.platform.archive.common.modules

import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSAsync}
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import uk.ac.wellcome.messaging.sqs.{SQSClientFactory, SQSConfig}
import uk.ac.wellcome.platform.archive.common.models.EnrichConfig

object SqsClientModule extends AbstractModule {
  import EnrichConfig._

  import scala.concurrent.duration._

  @Singleton
  @Provides
  def providesSqsConfig(config: Config) = {
    val url = config
      .required[String]("aws.sqs.queue.url")

    val waitTime = config
      .getOrElse[Int]("aws.sqs.queue.waitTime")(10) seconds

    val maxMessages = config
      .getOrElse[Int]("aws.sqs.queue.maxMessages")(10)

    val parallelism = config
      .getOrElse[Int]("aws.sqs.queue.parallelism")(10)

    SQSConfig(url, waitTime, maxMessages, parallelism)
  }

  @Singleton
  @Provides
  def providesSqsClientConfig(config: Config) = {
    val key = config
      .get[String]("aws.sqs.key")

    val secret = config
      .get[String]("aws.sqs.secret")

    val endpoint = config
      .get[String]("aws.sqs.endpoint")

    val region = config
      .getOrElse[String]("aws.sqs.region")("eu-west-1")

    SQSClientConfig(key, secret, endpoint, region)
  }

  @Singleton
  @Provides
  def providesSQSClient(sqsClientConfig: SQSClientConfig): AmazonSQS =
    SQSClientFactory.createSyncClient(
      region = sqsClientConfig.region,
      endpoint = sqsClientConfig.endpoint.getOrElse(""),
      accessKey = sqsClientConfig.accessKey.getOrElse(""),
      secretKey = sqsClientConfig.secretKey.getOrElse(""),
    )

  @Singleton
  @Provides
  def providesSQSAsyncClient(sqsClientConfig: SQSClientConfig): AmazonSQSAsync =
    SQSClientFactory.createAsyncClient(
      region = sqsClientConfig.region,
      endpoint = sqsClientConfig.endpoint.getOrElse(""),
      accessKey = sqsClientConfig.accessKey.getOrElse(""),
      secretKey = sqsClientConfig.secretKey.getOrElse(""),
    )
}

case class SQSClientConfig(
  accessKey: Option[String],
  secretKey: Option[String],
  endpoint: Option[String],
  region: String
)
