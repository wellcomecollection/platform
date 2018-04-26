package uk.ac.wellcome.test.fixtures


import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.cloudwatch.{AmazonCloudWatch, AmazonCloudWatchClientBuilder}
import com.twitter.inject.Logging
import org.scalatest.concurrent.Eventually

import scala.concurrent.duration._

trait CloudWatch extends Logging with ImplicitLogging {
  protected val awsNamespace: String = "test"

  private val localCloudWatchEndpointUrl: String = "http://localhost:4582"
  private val regionName: String = "localhost"

  protected val flushInterval: FiniteDuration = 1 second

  private val accessKey: String = "accessKey1"
  private val secretKey: String = "verySecretKey1"

  private val credentials = new AWSStaticCredentialsProvider(
    new BasicAWSCredentials(accessKey, secretKey))

  val cloudWatchClient: AmazonCloudWatch = AmazonCloudWatchClientBuilder
    .standard()
    .withCredentials(credentials)
    .withEndpointConfiguration(
      new EndpointConfiguration(localCloudWatchEndpointUrl, regionName))
    .build()

  def withCloudWatchClient[R] =
    fixture[AmazonCloudWatch, R](
      create = AmazonCloudWatchClientBuilder
        .standard()
        .withCredentials(credentials)
        .withEndpointConfiguration(
          new EndpointConfiguration(localCloudWatchEndpointUrl, regionName))
        .build()
    )

}