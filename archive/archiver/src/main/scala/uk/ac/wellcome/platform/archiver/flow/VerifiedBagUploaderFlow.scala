package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.{ExecutionContext, Future}

// TODO: Verify checksums in S3 are what you set them to
object VerifiedBagUploaderFlow extends Logging {
  def apply(config: BagUploaderConfig, zipFile: ZipFile)(
    implicit
    materializer: ActorMaterializer,
    s3Client: S3Client,
    executionContext: ExecutionContext
  ): Source[Future[Done], NotUsed] = {
    BagNameSource(zipFile).flatMapConcat(bagName => {

      debug(s"Found bag: $bagName")

      val bagDigestItemFlow = BagDigestItemFlow(config, bagName, zipFile)
      val archiveItemFlow = ArchiveItemFlow(zipFile, config)

      Source.fromIterator(() => config.digestNames.toIterator)
        .map(digestName => ObjectLocation(bagName, digestName))
        .map(digestLocation => {

          debug(s"Looking for digest location: $digestLocation")

          digestLocation
        })
        .via(bagDigestItemFlow)
        .via(archiveItemFlow)

    })
  }
}

object BagNameSource extends Logging {
  def apply(zipFile: ZipFile): Source[String, NotUsed] = {
    val entries = zipFile.entries()

    val tld = Stream
      .continually(entries.nextElement)
      .map(_.getName.split("/"))
      .flatMap(_.headOption)
      .filterNot(_.equals("."))
      .takeWhile(_ => entries.hasMoreElements)
      .toSet

    Source.fromIterator(() => tld.toIterator)
  }
}