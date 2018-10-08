package uk.ac.wellcome.platform.archive.archivist.modules

import com.google.inject.{AbstractModule, Provides}
import com.typesafe.config.Config
import uk.ac.wellcome.platform.archive.common.models.EnrichConfig

object BagUploaderConfigModule extends AbstractModule {
  import EnrichConfig._

  @Provides
  def providesBagUploaderConfig(config: Config) = {

    val uploadNamespace = config
      .required[String]("upload.namespace")

    val parallelism = config
      .getOrElse[Int]("upload.parallelism")(10)

    val uploadPrefix = config
      .getOrElse[String]("upload.prefix")("archive")

    BagUploaderConfig(
      UploadConfig(
        uploadNamespace,
        uploadPrefix
      ),
      BagItConfig(),
      parallelism
    )
  }
}

case class BagUploaderConfig(
  uploadConfig: UploadConfig,
  bagItConfig: BagItConfig = BagItConfig(),
  parallelism: Int
)

case class UploadConfig(uploadNamespace: String,
                        uploadPrefix: String = "archive")

case class BagItConfig(digestDelimiterRegexp: String = " +",
                       tagManifestFilePattern: String = "tagmanifest-%s.txt",
                       manifestFilePattern: String = "manifest-%s.txt",
                       algorithm: String = "sha256") {

  def tagManifestFileName =
    tagManifestFilePattern.format(algorithm)

  def manifestFileName =
    manifestFilePattern.format(algorithm)

  def digestNames = List(tagManifestFileName, manifestFileName)
}
