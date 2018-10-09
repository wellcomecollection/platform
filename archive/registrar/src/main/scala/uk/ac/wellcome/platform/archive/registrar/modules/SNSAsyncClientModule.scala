package uk.ac.wellcome.platform.archive.registrar.modules

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sns.{AmazonSNSAsync, AmazonSNSAsyncClientBuilder}
import com.google.inject.{AbstractModule, Provides, Singleton}
import uk.ac.wellcome.platform.archive.common.modules.SnsClientConfig

object SNSAsyncClientModule extends AbstractModule {
  @Singleton
  @Provides
  def providesSNSAsyncClient(
    snsClientConfig: SnsClientConfig): AmazonSNSAsync = {
    val endpoint = snsClientConfig.endpoint.getOrElse("")
    if (endpoint.isEmpty) {
      AmazonSNSAsyncClientBuilder
        .standard()
        .withRegion(snsClientConfig.region)
        .build()
    } else {
      val accessKey = snsClientConfig.accessKey.getOrElse(
        throw new RuntimeException("accessKey required"))
      val secretKey = snsClientConfig.secretKey.getOrElse(
        throw new RuntimeException("secretKey required"))
      AmazonSNSAsyncClientBuilder
        .standard()
        .withCredentials(new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(accessKey, secretKey)))
        .withEndpointConfiguration(
          new EndpointConfiguration(endpoint, snsClientConfig.region))
        .build()
    }
  }
}
