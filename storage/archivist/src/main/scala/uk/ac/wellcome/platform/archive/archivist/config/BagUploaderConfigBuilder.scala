package uk.ac.wellcome.platform.archive.archivist.config

import com.typesafe.config.Config
import uk.ac.wellcome.platform.archive.archivist.models.{
  BagItConfig,
  BagUploaderConfig,
  UploadConfig
}
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._

object BagUploaderConfigBuilder {
  def buildBagUploaderConfig(config: Config): BagUploaderConfig = {
    BagUploaderConfig(
      uploadConfig = UploadConfig(
        uploadNamespace = config.required[String]("upload.namespace"),
        uploadPrefix =
          config.getOrElse[String]("upload.prefix")(default = "archive")
      ),
      bagItConfig = BagItConfig(),
      parallelism = config.getOrElse[Int]("uploader.parallelism")(default = 10)
    )
  }
}
