package uk.ac.wellcome.platform.archive.archivist.builders

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer.{TransferManager, TransferManagerBuilder}
import uk.ac.wellcome.config.core.builders.AWSClientConfigBuilder

object TFBuilder extends AWSClientConfigBuilder {
  def buildTransferManager(s3Client: AmazonS3): TransferManager =
    TransferManagerBuilder.standard
      .withS3Client(s3Client)
      .build
}
