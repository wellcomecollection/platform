package uk.ac.wellcome.platform.archive.common.config

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.platform.archive.common.modules.SnsClientConfig

trait SnsClientConfigurator extends ScallopConf {
  val arguments: Seq[String]

  private val awsSnsAccessKey = opt[String]()
  private val awsSnsSecretKey = opt[String]()
  private val awsSnsRegion = opt[String](default = Some("eu-west-1"))
  private val awsSnsEndpoint = opt[String]()

  verify()

  val snsClientConfig = SnsClientConfig(
    accessKey = awsSnsAccessKey.toOption,
    secretKey = awsSnsSecretKey.toOption,
    region = awsSnsRegion(),
    endpoint = awsSnsEndpoint.toOption
  )
}
