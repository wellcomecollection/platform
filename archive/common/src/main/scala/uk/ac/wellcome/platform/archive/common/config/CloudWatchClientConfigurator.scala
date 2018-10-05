package uk.ac.wellcome.platform.archive.common.config

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.platform.archive.common.modules.config.CloudwatchConfig

trait CloudWatchClientConfigurator extends ScallopConf {
  val arguments: Seq[String]

  private val awsCloudwatchRegion = opt[String](default = Some("eu-west-1"))
  private val awsCloudwatchEndpoint = opt[String]()

  verify()

  val cloudwatchClientConfig = CloudwatchConfig(
    region = awsCloudwatchRegion(),
    endpoint = awsCloudwatchEndpoint.toOption
  )
}
