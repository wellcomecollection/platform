package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.{Done, NotUsed}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Source
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.Future

// TODO: Verify checksums in S3 are what you set them to
object VerifiedBagUploaderFlow {
  def apply(config: BagUploaderConfig, zipFile: ZipFile, bagName: String)(
    implicit
      materializer: ActorMaterializer,
      s3Client: S3Client
  ): Source[Future[Done], NotUsed] = {
    val bagDigestItemFlow = BagDigestItemFlow(config, bagName, zipFile)
    val archiveItemFlow = ArchiveItemFlow(zipFile, config)

    Source.fromIterator(() => config.digestNames.toIterator)
      .map(digestName => ObjectLocation(bagName, digestName))
      .via(bagDigestItemFlow)
      .via(archiveItemFlow)
  }
}
