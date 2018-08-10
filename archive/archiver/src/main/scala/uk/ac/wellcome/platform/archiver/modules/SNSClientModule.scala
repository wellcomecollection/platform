package uk.ac.wellcome.platform.archiver.modules

import com.amazonaws.services.sns.AmazonSNS
import com.google.inject.{AbstractModule, Provides, Singleton}
import uk.ac.wellcome.messaging.sns.SNSClientFactory
import uk.ac.wellcome.platform.archiver.models.SnsClientConfig

object SNSClientModule extends AbstractModule {
  @Singleton
  @Provides
  def providesSNSAsyncClient(snsClientConfig: SnsClientConfig): AmazonSNS =
    SNSClientFactory.create(
      region = snsClientConfig.region,
      endpoint = snsClientConfig.endpoint.getOrElse(""),
      accessKey = snsClientConfig.accessKey.getOrElse(""),
      secretKey = snsClientConfig.secretKey.getOrElse(""),
    )
}
