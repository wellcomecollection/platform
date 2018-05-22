package uk.ac.wellcome.finatra.messaging

import com.amazonaws.services.sns.AmazonSNS
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton
import uk.ac.wellcome.messaging.sns.SNSClientFactory

object SNSClientModule extends TwitterModule {
  val snsEndpoint = flag[String](
    "aws.sns.endpoint",
    "",
    "Endpoint of AWS SNS. The region will be used if the enpoint is not provided")

  private val accessKey =
    flag[String]("aws.sns.accessKey", "", "AccessKey to access SNS")
  private val secretKey =
    flag[String]("aws.sns.secretKey", "", "SecretKey to access SNS")

  private val region = flag[String](name = "aws.sns.region", default = "eu-west-1", help = "AWS region for sns")

  @Singleton
  @Provides
  def providesSNSClient(): AmazonSNS =
    SNSClientFactory.create(
      region = region(),
      endpoint = snsEndpoint(),
      accessKey = accessKey(),
      secretKey = secretKey()
    )

}
