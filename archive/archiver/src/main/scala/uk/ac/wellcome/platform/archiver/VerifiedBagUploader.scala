package uk.ac.wellcome.platform.archiver

import java.util.zip.ZipFile

import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Sink, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.flow.{ArchiveItemFlow, BagDigestItemFlow}
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation


class VerifiedBagUploader(config: BagUploaderConfig)(
  implicit
    materializer: ActorMaterializer,
    s3Client: S3Client
) extends Logging {

  def verify(zipFile: ZipFile, bagName: String) = {
    val bagDigestItemFlow = BagDigestItemFlow(config, bagName, zipFile)
    val archiveItemFlow = ArchiveItemFlow(zipFile, config)

    Source.fromIterator(() => config.digestNames.toIterator)
      .map(digestName => ObjectLocation(bagName, digestName))
      .via(bagDigestItemFlow)
      .via(archiveItemFlow)
      .runWith(Sink.ignore)
  }
}

// on download verify checksums are what you set them to
