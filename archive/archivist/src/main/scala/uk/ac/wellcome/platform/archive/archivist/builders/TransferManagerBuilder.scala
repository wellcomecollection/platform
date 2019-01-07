package uk.ac.wellcome.platform.archive.archivist.builders

import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.{transfer, AmazonS3}
import uk.ac.wellcome.config.core.builders.AWSClientConfigBuilder

object TransferManagerBuilder extends AWSClientConfigBuilder {
  def buildTransferManager(s3Client: AmazonS3): TransferManager =
    transfer.TransferManagerBuilder.standard
      .withS3Client(s3Client)
      .build
}
