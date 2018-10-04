package uk.ac.wellcome.platform.archive.common.config

import org.rogach.scallop.{ScallopConf, ScallopOption}
import uk.ac.wellcome.messaging.sns.SNSConfig

trait SnsConfigConfigurator extends ScallopConf {
  val arguments: Seq[String]

  private val snsTopicArn: ScallopOption[String] = opt[String](required = true)

  verify()

  val snsConfig = SNSConfig(
    topicArn = snsTopicArn()
  )
}
