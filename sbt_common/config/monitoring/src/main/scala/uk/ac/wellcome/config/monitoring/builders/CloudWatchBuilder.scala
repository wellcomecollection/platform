package uk.ac.wellcome.config.monitoring.builders

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.typesafe.config.Config
import uk.ac.wellcome.config.core.builders.AWSClientConfigBuilder
import uk.ac.wellcome.config.core.models.AWSClientConfig
import uk.ac.wellcome.monitoring.CloudWatchClientFactory

object CloudWatchBuilder extends AWSClientConfigBuilder {
  private def buildCloudWatchClient(
    awsClientConfig: AWSClientConfig): AmazonCloudWatch =
    CloudWatchClientFactory.create(
      region = awsClientConfig.region,
      endpoint = awsClientConfig.endpoint.getOrElse("")
    )

  def buildCloudWatchClient(config: Config): AmazonCloudWatch =
    buildCloudWatchClient(
      awsClientConfig = buildAWSClientConfig(config, namespace = "cloudwatch")
    )
}
