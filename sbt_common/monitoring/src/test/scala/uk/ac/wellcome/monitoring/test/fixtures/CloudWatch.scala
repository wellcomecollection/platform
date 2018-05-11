package uk.ac.wellcome.monitoring.test.fixtures

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import uk.ac.wellcome.models.aws.AWSConfig
import uk.ac.wellcome.monitoring.CloudWatchClientModule

import scala.concurrent.duration._

trait CloudWatch {
  protected val awsNamespace: String = "test"

  protected val localCloudWatchEndpointUrl: String = "http://localhost:4582"
  private val regionName: String = "eu-west-1"

  protected val flushInterval: FiniteDuration = 1 second

  def cloudWatchLocalFlags =
    Map(
      "aws.cloudWatch.endpoint" -> localCloudWatchEndpointUrl
    )

  val cloudWatchClient: AmazonCloudWatch =
    CloudWatchClientModule.buildCloudWatchClient(
      awsConfig = AWSConfig(region = regionName),
      endpoint = localCloudWatchEndpointUrl
    )
}
