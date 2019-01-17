package uk.ac.wellcome.platform.archive.archivist.models

case class UploadConfig(uploadNamespace: String,
                        uploadPrefix: String = "archive")

case class BagItConfig(tagManifestFilePattern: String = "tagmanifest-%s.txt",
                       manifestFilePattern: String = "manifest-%s.txt",
                       algorithm: String = "sha256") {

  def tagManifestFileName =
    tagManifestFilePattern.format(algorithm)

  def manifestFileName =
    manifestFilePattern.format(algorithm)

  def digestNames = List(tagManifestFileName, manifestFileName)
}

case class BagUploaderConfig(
  uploadConfig: UploadConfig,
  bagItConfig: BagItConfig = BagItConfig(),
  parallelism: Int
)
