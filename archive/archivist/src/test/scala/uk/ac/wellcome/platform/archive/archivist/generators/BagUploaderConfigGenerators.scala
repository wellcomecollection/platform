package uk.ac.wellcome.platform.archive.archivist.generators

import uk.ac.wellcome.platform.archive.archivist.models.{
  BagItConfig,
  BagUploaderConfig,
  UploadConfig
}
import uk.ac.wellcome.storage.fixtures.S3.Bucket

trait BagUploaderConfigGenerators {
  def createBagUploaderConfigWith(bucket: Bucket): BagUploaderConfig =
    createBagUploaderConfigWith(bucket.name)

  def createBagUploaderConfigWith(bucketName: String): BagUploaderConfig =
    BagUploaderConfig(
      uploadConfig = UploadConfig(
        uploadNamespace = bucketName
      ),
      parallelism = 10,
      bagItConfig = BagItConfig()
    )
}
