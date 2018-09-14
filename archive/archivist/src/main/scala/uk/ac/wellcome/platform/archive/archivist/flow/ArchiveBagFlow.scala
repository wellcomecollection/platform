package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.ZipFile

import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import uk.ac.wellcome.platform.archive.archivist.models.BagItConfig
import uk.ac.wellcome.platform.archive.common.models.{BagLocation, BagName}
import uk.ac.wellcome.storage.ObjectLocation

object ArchiveBagFlow {

  def apply(archiveJob: ArchiveJob)(
    implicit
    s3Client: S3Client
  ): Source[ArchiveJob, NotUsed] = {

    val bagDigestItemFlow =
      BagDigestItemFlow(
        archiveJob.config.digestDelimiterRegexp
      )
    val archiveItemFlow =
      ArchiveItemFlow()
    val digestLocations =
      digestNames(
        archiveJob.bagLocation.bagName,
        archiveJob.config.digestNames
      )

    Source
      .fromIterator(() => digestLocations)
      .log("digest location")
      .map((_, archiveJob.bagLocation.bagName, archiveJob.zipFile))
      .via(bagDigestItemFlow)
      .log("bag digest item")
      .map((archiveJob.bagLocation, _, archiveJob.zipFile))
      .via(archiveItemFlow)
      .map(_ => archiveJob)
  }

  private def digestNames(bagName: BagName, digestNames: List[String]) =
    digestNames
      .map(digestName => {
        ObjectLocation(bagName.value, digestName)
      })
      .toIterator
}

case class ArchiveJob(zipFile: ZipFile, bagLocation: BagLocation, config: BagItConfig)

