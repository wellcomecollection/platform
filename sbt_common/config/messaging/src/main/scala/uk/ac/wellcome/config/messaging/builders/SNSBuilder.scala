package uk.ac.wellcome.config.messaging.builders

import com.amazonaws.services.sns.AmazonSNS
import com.typesafe.config.Config
import uk.ac.wellcome.config.core.builders.EnrichConfig._
import uk.ac.wellcome.config.core.builders.{AWSClientConfigBuilder, AkkaBuilder}
import uk.ac.wellcome.config.core.models.AWSClientConfig
import uk.ac.wellcome.messaging.sns.{
  SNSClientFactory,
  SNSConfig,
  SNSMessageWriter,
  SNSWriter
}

object SNSBuilder extends AWSClientConfigBuilder {
  def buildSNSConfig(config: Config, namespace: String = ""): SNSConfig = {
    val topicArn = config
      .required[String](s"aws.$namespace.sns.topic.arn")

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

  def buildSNSMessageWriter(config: Config): SNSMessageWriter =
    new SNSMessageWriter(snsClient = buildSNSClient(config))(
      ec = AkkaBuilder.buildExecutionContext())

  def buildSNSWriter(config: Config): SNSWriter =
    new SNSWriter(
      snsMessageWriter = buildSNSMessageWriter(config),
      snsConfig = buildSNSConfig(config)
    )
}
