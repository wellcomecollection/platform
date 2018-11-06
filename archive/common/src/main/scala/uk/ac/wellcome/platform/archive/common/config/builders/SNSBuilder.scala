package uk.ac.wellcome.platform.archive.common.config.builders

import com.typesafe.config.Config
import uk.ac.wellcome.messaging.sns.{SNSClientFactory, SNSConfig}
import EnrichConfig._
import com.amazonaws.services.sns.AmazonSNS
import uk.ac.wellcome.platform.archive.common.config.models.SNSClientConfig

object SNSBuilder {
  def buildSNSConfig(config: Config): SNSConfig = {
    val topicArn = config
      .required[String]("aws.sns.topic.arn")

    SNSConfig(topicArn = topicArn)
  }

  def buildSNSClientConfig(config: Config): SNSClientConfig = {
    val accessKey = config.get[String]("aws.sns.key")
    val secretKey = config.get[String]("aws.sns.secret")
    val endpoint = config.get[String]("aws.sns.endpoint")
    val region = config.getOrElse[String]("aws.sns.region")("eu-west-1")

    SNSClientConfig(
      accessKey = accessKey,
      secretKey = secretKey,
      endpoint = endpoint,
      region = region
    )
  }

  def buildSNSClient(snsClientConfig: SNSClientConfig): AmazonSNS =
    SNSClientFactory.create(
      region = snsClientConfig.region,
      endpoint = snsClientConfig.endpoint.getOrElse(""),
      accessKey = snsClientConfig.accessKey.getOrElse(""),
      secretKey = snsClientConfig.secretKey.getOrElse("")
    )

  def buildSNSClient(config: Config): AmazonSNS =
    buildSNSClient(
      snsClientConfig = buildSNSClientConfig(config)
    )
}

