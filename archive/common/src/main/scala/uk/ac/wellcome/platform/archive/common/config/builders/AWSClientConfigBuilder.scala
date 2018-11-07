package uk.ac.wellcome.platform.archive.common.config.builders

import com.typesafe.config.Config
import uk.ac.wellcome.platform.archive.common.config.models.AWSClientConfig

import EnrichConfig._

trait AWSClientConfigBuilder {
  protected def buildAWSClientConfig(config: Config,
                                     namespace: String): AWSClientConfig = {
    val accessKey = config.get[String](s"aws.$namespace.key")
    val secretKey = config.get[String](s"aws.$namespace.secret")
    val endpoint = config.get[String](s"aws.$namespace.endpoint")
    val region = config.getOrElse[String](s"aws.$namespace.region")("eu-west-1")

    AWSClientConfig(
      accessKey = accessKey,
      secretKey = secretKey,
      endpoint = endpoint,
      region = region
    )
  }
}
