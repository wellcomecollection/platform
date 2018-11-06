package uk.ac.wellcome.platform.archive.common.config.builders

import com.typesafe.config.Config
import uk.ac.wellcome.messaging.sqs.{SQSClientFactory, SQSConfig}
import uk.ac.wellcome.platform.archive.common.config.models.{AWSClientConfig, SQSClientConfig}
import EnrichConfig._
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSAsync}

object SQSBuilder extends AWSClientConfigBuilder {
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

  private def buildSQSClient(awsClientConfig: AWSClientConfig): AmazonSQS =
    SQSClientFactory.createSyncClient(
      region = awsClientConfig.region,
      endpoint = awsClientConfig.endpoint.getOrElse(""),
      accessKey = awsClientConfig.accessKey.getOrElse(""),
      secretKey = awsClientConfig.secretKey.getOrElse("")
    )

  def buildSQSClient(config: Config): AmazonSQS =
    buildSQSClient(
      awsClientConfig = buildAWSClientConfig(config, namespace = "sqs")
    )

  def buildSQSAsyncClient(awsClientConfig: AWSClientConfig): AmazonSQSAsync =
    SQSClientFactory.createAsyncClient(
      region = awsClientConfig.region,
      endpoint = awsClientConfig.endpoint.getOrElse(""),
      accessKey = awsClientConfig.accessKey.getOrElse(""),
      secretKey = awsClientConfig.secretKey.getOrElse("")
    )

  def buildSQSAsyncClient(config: Config): AmazonSQSAsync =
    buildSQSAsyncClient(
      awsClientConfig = buildAWSClientConfig(config, namespace = "sqs")
    )
}
