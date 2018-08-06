package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.event.Logging
import akka.stream.{ActorMaterializer, Attributes}
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.{Done, NotUsed}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation

// TODO: Verify checksums in S3 are what you set them to
object VerifiedBagUploaderFlow extends Logging {
  def apply(config: BagUploaderConfig)(
    implicit
    materializer: ActorMaterializer,
    s3Client: S3Client
  ): Flow[ZipFile, Seq[Done], NotUsed] = {

    val bagNameFlow: Flow[ZipFile, String, NotUsed] = BagNameFlow()
    val bagDigestItemFlow: Flow[(ObjectLocation, String, ZipFile), BagDigestItem, NotUsed] = BagDigestItemFlow(config)
    val archiveItemFlow: Flow[(BagDigestItem, ZipFile), Done, NotUsed] = ArchiveItemFlow(config)

    Flow[ZipFile].flatMapConcat((zipFile) => {
      Source.single(zipFile).via(bagNameFlow).flatMapConcat(bagName => {

        val digestLocationSource = Source.fromIterator(() =>
          config.digestNames.map(digestName => {
            ObjectLocation(bagName, digestName)
          }).toIterator
        )

        val archiveFlow: Flow[ObjectLocation, Done, NotUsed] = Flow[ObjectLocation]
          .log("digest location")
          .map(location => (location, bagName, zipFile))
          .via(bagDigestItemFlow)
          .log("bag digest item")
          .map(bagDigestItem => (bagDigestItem, zipFile))
          .via(archiveItemFlow)
          .withAttributes(
            Attributes.logLevels(
              onElement = Logging.WarningLevel,
              onFinish = Logging.InfoLevel,
              onFailure = Logging.DebugLevel
            ))

        Source.fromFuture(digestLocationSource.via(archiveFlow).runWith(Sink.seq))
      })
    })
  }
}