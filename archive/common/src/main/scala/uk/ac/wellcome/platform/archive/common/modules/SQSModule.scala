package uk.ac.wellcome.platform.archive.common.modules

import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSAsync}
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import uk.ac.wellcome.messaging.sqs.{SQSClientFactory, SQSConfig}
import uk.ac.wellcome.platform.archive.common.models.EnrichConfig._

object SQSModule extends AbstractModule {
  install(TypesafeConfigModule)

  @Singleton
  @Provides
  def providesSQSConfig(config: Config): SQSConfig = {
    val queueUrl = config
      .required[String]("aws.sqs.queue.url")
    val parallelism = config
      .getOrElse[Int]("aws.sqs.queue.parallelism")(default = 10)

    SQSConfig(
      queueUrl = queueUrl,
      parallelism = parallelism
    )
  }

  @Singleton
  @Provides
  def providesSQSClientConfig(config: Config): SQSClientConfig = {
    val accessKey = config.get[String]("aws.sqs.key")
    val secretKey = config.get[String]("aws.sqs.secret")
    val endpoint = config.get[String]("aws.sqs.endpoint")
    val region = config.getOrElse[String]("aws.sqs.region")("eu-west-1")

    SQSClientConfig(
      accessKey = accessKey,
      secretKey = secretKey,
      endpoint = endpoint,
      region = region
    )
  }

  @Singleton
  @Provides
  def providesSQSClient(sqsClientConfig: SQSClientConfig): AmazonSQS =
    SQSClientFactory.createSyncClient(
      region = sqsClientConfig.region,
      endpoint = sqsClientConfig.endpoint.getOrElse(""),
      accessKey = sqsClientConfig.accessKey.getOrElse(""),
      secretKey = sqsClientConfig.secretKey.getOrElse("")
    )

  @Singleton
  @Provides
  def providesSQSAsyncClient(sqsClientConfig: SQSClientConfig): AmazonSQSAsync =
    SQSClientFactory.createAsyncClient(
      region = sqsClientConfig.region,
      endpoint = sqsClientConfig.endpoint.getOrElse(""),
      accessKey = sqsClientConfig.accessKey.getOrElse(""),
      secretKey = sqsClientConfig.secretKey.getOrElse("")
    )
}
