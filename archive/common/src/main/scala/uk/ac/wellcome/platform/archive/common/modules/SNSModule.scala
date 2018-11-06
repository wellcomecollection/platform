package uk.ac.wellcome.platform.archive.common.modules

import com.amazonaws.services.sns.AmazonSNS
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import uk.ac.wellcome.messaging.sns.{SNSClientFactory, SNSConfig}
import uk.ac.wellcome.platform.archive.common.models.EnrichConfig._

object SNSModule extends AbstractModule {
  install(TypesafeConfigModule)

  @Singleton
  @Provides
  def providesSNSConfig(config: Config): SNSConfig = {
    val topicArn = config
      .required[String]("aws.sns.topic.arn")

    SNSConfig(topicArn = topicArn)
  }

  @Singleton
  @Provides
  def providesSNSClientConfig(config: Config): SnsClientConfig = {
    val accessKey = config.get[String]("aws.sns.key")
    val secretKey = config.get[String]("aws.sns.secret")
    val endpoint = config.get[String]("aws.sns.endpoint")
    val region = config.getOrElse[String]("aws.sns.region")("eu-west-1")

    SnsClientConfig(
      accessKey = accessKey,
      secretKey = secretKey,
      endpoint = endpoint,
      region = region
    )
  }

  @Singleton
  @Provides
  def providesSNSClient(snsClientConfig: SnsClientConfig): AmazonSNS =
    SNSClientFactory.create(
      region = snsClientConfig.region,
      endpoint = snsClientConfig.endpoint.getOrElse(""),
      accessKey = snsClientConfig.accessKey.getOrElse(
        throw new RuntimeException("accessKey required")
      ),
      secretKey = snsClientConfig.secretKey.getOrElse(
        throw new RuntimeException("secretKey required")
      )
    )
}
