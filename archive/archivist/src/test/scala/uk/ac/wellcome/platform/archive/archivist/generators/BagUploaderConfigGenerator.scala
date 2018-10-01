package uk.ac.wellcome.platform.archive.archivist.generators
import uk.ac.wellcome.platform.archive.archivist.models.{
  BagItConfig,
  BagUploaderConfig,
  UploadConfig
}
import uk.ac.wellcome.storage.fixtures.S3.Bucket

trait BagUploaderConfigGenerator {
  def createBagUploaderConfig(bucket: Bucket) =
    BagUploaderConfig(
      uploadConfig = UploadConfig(
        uploadNamespace = bucket.name
      ),
      parallelism = 10,
      bagItConfig = BagItConfig()
    )
}
