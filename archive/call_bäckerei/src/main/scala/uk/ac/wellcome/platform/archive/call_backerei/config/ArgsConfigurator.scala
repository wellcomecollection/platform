package uk.ac.wellcome.platform.archive.call_backerei.config

import org.rogach.scallop.{ScallopConf, ScallopOption}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.call_backerei.models.CallBackereiConfig
import uk.ac.wellcome.platform.archive.common.config._

class ArgsConfigurator(val arguments: Seq[String])
  extends ScallopConf(arguments)
  with CloudWatchClientConfigurator
  with MetricsConfigConfigurator
  with SnsClientConfigurator
  with SqsClientConfigurator
  with SqsConfigConfigurator {

  val topicArn: ScallopOption[String] = opt[String](required = true)

  verify()

  val snsConfig = SNSConfig(
    topicArn(),
  )

  val appConfig = CallBackereiConfig(
    cloudwatchClientConfig = cloudwatchClientConfig,
    sqsClientConfig = sqsClientConfig,
    sqsConfig = sqsConfig,
    snsClientConfig = snsClientConfig,
    snsConfig = snsConfig,
    metricsConfig = metricsConfig
  )
}
