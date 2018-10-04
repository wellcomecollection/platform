package uk.ac.wellcome.platform.archive.archivist.config

import org.rogach.scallop.{ScallopConf, ScallopOption}
import uk.ac.wellcome.messaging.sns.SNSConfig

trait RegistrarSnsConfigConfigurator extends ScallopConf {
  val arguments: Seq[String]

  private val registrarSnsTopicArn: ScallopOption[String] =
    opt[String](required = true)

  verify()

  val registrarSnsConfig = SNSConfig(
    topicArn = registrarSnsTopicArn()
  )
}
