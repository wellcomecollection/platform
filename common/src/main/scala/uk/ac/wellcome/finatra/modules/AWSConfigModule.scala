package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.AWSConfig

object AWSConfigModule extends TwitterModule {
  private val region = flag[String]("aws.region", "eu-west-1", "AWS region")
  private val awsAccessKey = flag[String](
    "aws.accessKey",
    "",
    "Access Key for AWS service. Needed if an endpoint for a service is specified")
  private val awsSecretKey = flag[String](
    "aws.secretKey",
    "",
    "Secret Key for AWS service. Needed if an endpoint for a service is specified")
  @Singleton
  @Provides
  def providesAWSConfig(): AWSConfig =
    AWSConfig(region())
}
