package uk.ac.wellcome.platform.archive.common.config.builders

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.typesafe.config.Config
import uk.ac.wellcome.monitoring.CloudWatchClientFactory
import uk.ac.wellcome.platform.archive.common.config.models.AWSClientConfig

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
