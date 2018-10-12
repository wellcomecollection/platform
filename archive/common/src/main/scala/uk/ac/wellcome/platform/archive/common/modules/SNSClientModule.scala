package uk.ac.wellcome.platform.archive.common.modules

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder._
import com.google.inject.{AbstractModule, Provides, Singleton}

object SNSClientModule extends AbstractModule {
  @Singleton
  @Provides
  def providesSNSClient(snsClientConfig: SnsClientConfig): AmazonSNS = {
    val endpoint = snsClientConfig.endpoint.getOrElse("")
    if (endpoint.isEmpty) {
      standard()
        .withRegion(snsClientConfig.region)
        .build()
    } else {
      val accessKey = snsClientConfig.accessKey.getOrElse(
        throw new RuntimeException("accessKey required"))
      val secretKey = snsClientConfig.secretKey.getOrElse(
        throw new RuntimeException("secretKey required"))
      standard()
        .withCredentials(new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(accessKey, secretKey)))
        .withEndpointConfiguration(
          new EndpointConfiguration(endpoint, snsClientConfig.region))
        .build()
    }
  }
}

case class SnsClientConfig(
  accessKey: Option[String],
  secretKey: Option[String],
  endpoint: Option[String],
  region: String
)
