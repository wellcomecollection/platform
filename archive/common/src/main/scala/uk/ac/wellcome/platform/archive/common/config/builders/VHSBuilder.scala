package uk.ac.wellcome.platform.archive.common.config.builders

import com.typesafe.config.Config
import uk.ac.wellcome.config.core.builders.EnrichConfig._
import uk.ac.wellcome.storage.vhs.VHSConfig

object VHSBuilder {
  def buildVHSConfig(config: Config): VHSConfig = {
    val s3Config = S3Builder.buildS3Config(config, namespace = "vhs")
    val dynamoConfig =
      DynamoBuilder.buildDynamoConfig(config, namespace = "vhs")

    val globalS3Prefix = config
      .required[String]("aws.vhs.s3.globalPrefix")

    VHSConfig(
      dynamoConfig = dynamoConfig,
      s3Config = s3Config,
      globalS3Prefix = globalS3Prefix
    )
  }
}
