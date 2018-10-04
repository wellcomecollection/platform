package uk.ac.wellcome.platform.archive.archivist.config

import org.rogach.scallop.{ScallopConf, ScallopOption}
import uk.ac.wellcome.messaging.sns.SNSConfig

trait ProgressSnsConfigConfigurator extends ScallopConf {
  val arguments: Seq[String]

  private val progressSnsTopicArn: ScallopOption[String] =
    opt[String](required = true)

  verify()

  val progressSnsConfig = SNSConfig(
    topicArn = progressSnsTopicArn()
  )
}
