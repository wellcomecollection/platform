package uk.ac.wellcome.platform.archive.archivist.generators

import uk.ac.wellcome.platform.archive.archivist.models.{
  BagItConfig,
  BagUploaderConfig,
  UploadConfig
}
import uk.ac.wellcome.storage.fixtures.S3.Bucket

trait BagUploaderConfigGenerators {
  def createBagUploaderConfigWith(bucket: Bucket,
                                  parallelism: Int = 10): BagUploaderConfig =
    BagUploaderConfig(
      uploadConfig = UploadConfig(
        uploadNamespace = bucket.name
      ),
      parallelism = parallelism,
      bagItConfig = BagItConfig()
    )
}
