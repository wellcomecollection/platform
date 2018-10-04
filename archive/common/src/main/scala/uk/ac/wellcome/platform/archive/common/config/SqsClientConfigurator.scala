package uk.ac.wellcome.platform.archive.common.config

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.platform.archive.common.modules.SQSClientConfig

trait SqsClientConfigurator extends ScallopConf {
  val arguments: Seq[String]

  private val awsSqsAccessKey = opt[String]()
  private val awsSqsSecretKey = opt[String]()
  private val awsSqsRegion = opt[String](default = Some("eu-west-1"))
  private val awsSqsEndpoint = opt[String]()

  verify()

  val sqsClientConfig = SQSClientConfig(
    accessKey = awsSqsAccessKey.toOption,
    secretKey = awsSqsSecretKey.toOption,
    region = awsSqsRegion(),
    endpoint = awsSqsEndpoint.toOption
  )
}
