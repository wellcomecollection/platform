package uk.ac.wellcome.platform.archive.common.config

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.platform.archive.common.modules.S3ClientConfig

trait S3ClientConfigurator extends ScallopConf {
  val arguments: Seq[String]

  private val awsS3AccessKey = opt[String]()
  private val awsS3SecretKey = opt[String]()
  private val awsS3Region = opt[String](default = Some("eu-west-1"))
  private val awsS3Endpoint = opt[String]()

  verify()

  val s3ClientConfig = S3ClientConfig(
    accessKey = awsS3AccessKey.toOption,
    secretKey = awsS3SecretKey.toOption,
    region = awsS3Region(),
    endpoint = awsS3Endpoint.toOption
  )
}
