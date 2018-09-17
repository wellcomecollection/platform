package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.ZipFile

import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Source
import uk.ac.wellcome.platform.archive.archivist.models.BagItConfig
import uk.ac.wellcome.platform.archive.common.models.{BagDigestItem, BagLocation, BagName}
import uk.ac.wellcome.storage.ObjectLocation

case class ArchiveItemJob(archiveJob: ArchiveJob, bagDigestItem: BagDigestItem) {
  def bagName = archiveJob.bagLocation.bagName

  def uploadLocation = createUploadLocation(archiveJob.bagLocation, bagDigestItem.location)

  private def createUploadLocation(
                                    bagLocation: BagLocation,
                                    itemLocation: ObjectLocation
                                  ) =
    ObjectLocation(
      bagLocation.storageNamespace,
      List(
        bagLocation.storagePath,
        bagLocation.bagName.value,
        itemLocation.key
      ).mkString("/")
    )
}

object ArchiveBagSource {

  def apply(archiveJob: ArchiveJob)(
    implicit
    s3Client: S3Client
  ) = {

    val bagDigestItemFlow = BagDigestItemFlow(
        archiveJob.config.digestDelimiterRegexp
      )
    val archiveItemFlow = ArchiveItemFlow()
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
      .map(ArchiveItemJob(archiveJob, _))
      .via(archiveItemFlow)
  }

  private def digestNames(bagName: BagName, digestNames: List[String]) =
    digestNames
      .map(digestName => {
        ObjectLocation(bagName.value, digestName)
      })
      .toIterator
}

case class ArchiveJob(zipFile: ZipFile, bagLocation: BagLocation, config: BagItConfig)

