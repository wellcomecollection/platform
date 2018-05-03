package uk.ac.wellcome.messaging.test.fixtures

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.cloudwatch.{
  AmazonCloudWatch,
  AmazonCloudWatchClientBuilder
}
import uk.ac.wellcome.test.fixtures.ImplicitLogging

import scala.concurrent.duration._

trait CloudWatch extends ImplicitLogging {
  protected val awsNamespace: String = "test"
  protected val flushInterval: FiniteDuration = 1 second

  protected val localCloudWatchEndpointUrl = "http://localhost:4582"
  protected val regionName = "eu-west-1"

  protected val accessKey: String = "accessKey1"
  protected val secretKey: String = "verySecretKey1"

  def cloudWatchLocalFlags =
    Map(
      "aws.cloudWatch.endpoint" -> localCloudWatchEndpointUrl
    )

  private val credentials = new AWSStaticCredentialsProvider(
    new BasicAWSCredentials(accessKey, secretKey))

  val cloudWatchClient: AmazonCloudWatch = AmazonCloudWatchClientBuilder
    .standard()
    .withCredentials(credentials)
    .withEndpointConfiguration(
      new EndpointConfiguration(localCloudWatchEndpointUrl, regionName))
    .build()
}
