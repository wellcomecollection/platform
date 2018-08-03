package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.ExecutionContext

// TODO: Verify checksums in S3 are what you set them to
object VerifiedBagUploaderFlow extends Logging {
  def apply(config: BagUploaderConfig)(
    implicit
    materializer: ActorMaterializer,
    s3Client: S3Client,
    executionContext: ExecutionContext
  ): Flow[ZipFile, ByteString, NotUsed] = {

    val bagNameFlow: Flow[ZipFile, String, NotUsed] = BagNameFlow()
    val bagDigestItemFlow: Flow[(ObjectLocation, String, ZipFile), BagDigestItem, NotUsed] = BagDigestItemFlow(config)
    val archiveItemFlow: Flow[(BagDigestItem, ZipFile), ByteString, NotUsed] = ArchiveItemFlow(config)

    Flow[ZipFile].flatMapConcat((zipFile) => {
      Source.single(zipFile).via(bagNameFlow).flatMapConcat(bagName => {

        val digestLocationSource = Source.fromIterator(() =>
          config.digestNames.map(digestName => {
            ObjectLocation(bagName, digestName)
          }).toIterator
        )

        val archiveFlow: Flow[ObjectLocation, ByteString, NotUsed] = Flow[ObjectLocation]
          .map(digestLocation => {
            debug(s"Found digest location: $digestLocation")
            digestLocation
          })
          .map(location => (location, bagName, zipFile))
          .via(bagDigestItemFlow)
          .map(bagDigestItem => (bagDigestItem, zipFile))
          .via(archiveItemFlow)

        digestLocationSource.via(archiveFlow)
      })
    })
  }
}