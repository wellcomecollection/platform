package uk.ac.wellcome.finatra.messaging

import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSAsync}
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton
import uk.ac.wellcome.messaging.sqs.SQSClientFactory
import uk.ac.wellcome.models.aws.AWSConfig

object SQSClientModule extends TwitterModule {
  override val modules = Seq(SQSConfigModule)

  val sqsEndpoint = flag[String](
    "aws.sqs.endpoint",
    "",
    "Endpoint for AWS SQS Service. If not provided, the region will be used instead")

  private val accessKey =
    flag[String]("aws.sqs.accessKey", "", "AccessKey to access SQS")
  private val secretKey =
    flag[String]("aws.sqs.secretKey", "", "SecretKey to access SQS")

  @Singleton
  @Provides
  def providesSQSClient(awsConfig: AWSConfig): AmazonSQS =
    SQSClientFactory.createSyncClient(
      region = awsConfig.region,
      endpoint = sqsEndpoint(),
      accessKey = accessKey(),
      secretKey = secretKey()
    )

  @Singleton
  @Provides
  def providesSQSAsyncClient(awsConfig: AWSConfig): AmazonSQSAsync =
    SQSClientFactory.createAsyncClient(
      region = awsConfig.region,
      endpoint = sqsEndpoint(),
      accessKey = accessKey(),
      secretKey = secretKey()
    )
}
