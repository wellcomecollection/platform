package uk.ac.wellcome.config.storage.builders

import com.typesafe.config.Config
import uk.ac.wellcome.config.core.builders.EnrichConfig._
import uk.ac.wellcome.storage.vhs.VHSConfig

object VHSBuilder {
  def buildVHSConfig(config: Config): VHSConfig = {
    val s3Config = S3Builder.buildS3Config(config, namespace = "vhs")
    val dynamoConfig =
      DynamoBuilder.buildDynamoConfig(config, namespace = "vhs")

    val globalS3Prefix = config
      .getOrElse[String]("aws.vhs.s3.globalPrefix")(default = "")

    VHSConfig(
      dynamoConfig = dynamoConfig,
      s3Config = s3Config,
      globalS3Prefix = globalS3Prefix
    )
  }
}
