package uk.ac.wellcome.platform.archive.common.modules

import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSAsync}
import com.google.inject.{AbstractModule, Provides, Singleton}
import uk.ac.wellcome.messaging.sqs.SQSClientFactory
import uk.ac.wellcome.platform.archive.common.config.models.SQSClientConfig

object SQSClientModule extends AbstractModule {

  @Singleton
  @Provides
  def providesSQSClient(sqsClientConfig: SQSClientConfig): AmazonSQS =
    SQSClientFactory.createSyncClient(
      region = sqsClientConfig.region,
      endpoint = sqsClientConfig.endpoint.getOrElse(""),
      accessKey = sqsClientConfig.accessKey.getOrElse(""),
      secretKey = sqsClientConfig.secretKey.getOrElse(""),
    )

  @Singleton
  @Provides
  def providesSQSAsyncClient(sqsClientConfig: SQSClientConfig): AmazonSQSAsync =
    SQSClientFactory.createAsyncClient(
      region = sqsClientConfig.region,
      endpoint = sqsClientConfig.endpoint.getOrElse(""),
      accessKey = sqsClientConfig.accessKey.getOrElse(""),
      secretKey = sqsClientConfig.secretKey.getOrElse(""),
    )
}
