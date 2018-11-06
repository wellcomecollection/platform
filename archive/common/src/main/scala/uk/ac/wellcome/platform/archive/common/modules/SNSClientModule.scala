package uk.ac.wellcome.platform.archive.common.modules

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder._
import com.google.inject.{AbstractModule, Provides, Singleton}
import uk.ac.wellcome.platform.archive.common.config.models.SNSClientConfig

object SNSClientModule extends AbstractModule {
  @Singleton
  @Provides
  def providesSNSClient(snsClientConfig: SNSClientConfig): AmazonSNS = {
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
