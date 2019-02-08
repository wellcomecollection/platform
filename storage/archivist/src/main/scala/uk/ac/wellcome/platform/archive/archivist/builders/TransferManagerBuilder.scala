package uk.ac.wellcome.platform.archive.archivist.builders

import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.{transfer, AmazonS3}
import com.typesafe.config.Config
import uk.ac.wellcome.storage.typesafe.S3Builder

object TransferManagerBuilder {
  def buildTransferManager(s3Client: AmazonS3): TransferManager =
    transfer.TransferManagerBuilder.standard
      .withS3Client(s3Client)
      .build

  def buildTransferManager(config: Config): TransferManager =
    buildTransferManager(S3Builder.buildS3Client(config))
}
