package uk.ac.wellcome.finatra.messaging

import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSAsync}
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton
import uk.ac.wellcome.messaging.sqs.SQSClientFactory

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

  private val region = flag[String](name = "aws.sqs.region", default = "eu-west-1", help = "AWS region for SQS")

  @Singleton
  @Provides
  def providesSQSClient(): AmazonSQS =
    SQSClientFactory.createSyncClient(
      region = region(),
      endpoint = sqsEndpoint(),
      accessKey = accessKey(),
      secretKey = secretKey()
    )

  @Singleton
  @Provides
  def providesSQSAsyncClient(): AmazonSQSAsync =
    SQSClientFactory.createAsyncClient(
      region = region(),
      endpoint = sqsEndpoint(),
      accessKey = accessKey(),
      secretKey = secretKey()
    )
}
