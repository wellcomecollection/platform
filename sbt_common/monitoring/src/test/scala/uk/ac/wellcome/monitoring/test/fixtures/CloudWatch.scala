package uk.ac.wellcome.monitoring.test.fixtures

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.cloudwatch.{AmazonCloudWatch, AmazonCloudWatchClientBuilder}
import uk.ac.wellcome.test.fixtures.ImplicitLogging

import scala.concurrent.duration._

trait CloudWatch extends ImplicitLogging {
  protected val awsNamespace: String = "test"

  protected val localCloudWatchEndpointUrl: String = "http://localhost:4582"
  private val regionName: String = "eu-west-1"

  protected val flushInterval: FiniteDuration = 1 second

  private val accessKey: String = "accessKey1"
  private val secretKey: String = "verySecretKey1"

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
