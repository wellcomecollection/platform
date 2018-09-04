package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.ZipFile

import akka.{Done, NotUsed}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Source
import uk.ac.wellcome.platform.archive.archivist.models.BagItConfig
import uk.ac.wellcome.platform.archive.common.models.{BagLocation, BagName}
import uk.ac.wellcome.storage.ObjectLocation

object ArchiveBagFlow {

  def apply(zipFile: ZipFile, bagLocation: BagLocation, config: BagItConfig)(
    implicit
    materializer: ActorMaterializer,
    s3Client: S3Client
  ): Source[Done, NotUsed] = {

    val bagDigestItemFlow = BagDigestItemFlow(config.digestDelimiterRegexp)
    val archiveItemFlow = ArchiveItemFlow()
    val digestLocations = digestNames(bagLocation.bagName, config.digestNames)

    Source
      .fromIterator(() => digestLocations)
      .log("digest location")
      .map((_, bagLocation.bagName, zipFile))
      .via(bagDigestItemFlow)
      .log("bag digest item")
      .map((bagLocation, _, zipFile))
      .via(archiveItemFlow)
  }

  private def digestNames(bagName: BagName, digestNames: List[String]) =
    digestNames
      .map(digestName => {
        ObjectLocation(bagName.value, digestName)
      })
      .toIterator
}
