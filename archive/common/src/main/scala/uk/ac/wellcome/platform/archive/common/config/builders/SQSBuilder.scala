package uk.ac.wellcome.platform.archive.common.config.builders

import com.typesafe.config.Config
import uk.ac.wellcome.messaging.sqs.{SQSClientFactory, SQSConfig}
import uk.ac.wellcome.platform.archive.common.config.models.SQSClientConfig
import EnrichConfig._
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSAsync}

object SQSBuilder {
  def buildSQSConfig(config: Config): SQSConfig = {
    val queueUrl = config
      .required[String]("aws.sqs.queue.url")
    val parallelism = config
      .getOrElse[Int]("aws.sqs.queue.parallelism")(default = 10)

    SQSConfig(
      queueUrl = queueUrl,
      parallelism = parallelism
    )
  }

  def buildSQSClientConfig(config: Config): SQSClientConfig = {
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

  def buildSQSClient(sqsClientConfig: SQSClientConfig): AmazonSQS =
    SQSClientFactory.createSyncClient(
      region = sqsClientConfig.region,
      endpoint = sqsClientConfig.endpoint.getOrElse(""),
      accessKey = sqsClientConfig.accessKey.getOrElse(""),
      secretKey = sqsClientConfig.secretKey.getOrElse("")
    )

  def buildSQSClient(config: Config): AmazonSQS =
    buildSQSClient(
      sqsClientConfig = buildSQSClientConfig(config)
    )

  def buildSQSAsyncClient(sqsClientConfig: SQSClientConfig): AmazonSQSAsync =
    SQSClientFactory.createAsyncClient(
      region = sqsClientConfig.region,
      endpoint = sqsClientConfig.endpoint.getOrElse(""),
      accessKey = sqsClientConfig.accessKey.getOrElse(""),
      secretKey = sqsClientConfig.secretKey.getOrElse("")
    )

  def buildSQSAsyncClient(config: Config): AmazonSQSAsync =
    buildSQSAsyncClient(
      sqsClientConfig = buildSQSClientConfig(config)
    )
}
