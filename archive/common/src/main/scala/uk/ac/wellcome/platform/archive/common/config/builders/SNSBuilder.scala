package uk.ac.wellcome.platform.archive.common.config.builders

import com.typesafe.config.Config
import uk.ac.wellcome.messaging.sns.{
  SNSClientFactory,
  SNSConfig,
  SNSMessageWriter,
  SNSWriter
}
import EnrichConfig._
import com.amazonaws.services.sns.AmazonSNS
import uk.ac.wellcome.platform.archive.common.config.models.AWSClientConfig

object SNSBuilder extends AWSClientConfigBuilder {
  def buildSNSConfig(config: Config): SNSConfig = {
    val topicArn = config
      .required[String]("aws.sns.topic.arn")

    SNSConfig(topicArn = topicArn)
  }

  private def buildSNSClient(awsClientConfig: AWSClientConfig): AmazonSNS =
    SNSClientFactory.create(
      region = awsClientConfig.region,
      endpoint = awsClientConfig.endpoint.getOrElse(""),
      accessKey = awsClientConfig.accessKey.getOrElse(""),
      secretKey = awsClientConfig.secretKey.getOrElse("")
    )

  def buildSNSClient(config: Config): AmazonSNS =
    buildSNSClient(
      awsClientConfig = buildAWSClientConfig(config, namespace = "sns")
    )

  def buildSNSWriter(config: Config): SNSWriter =
    new SNSWriter(
      snsMessageWriter = new SNSMessageWriter(
        snsClient = buildSNSClient(config)
      )(ec = AkkaBuilder.buildExecutionContext()),
      snsConfig = buildSNSConfig(config)
    )
}
