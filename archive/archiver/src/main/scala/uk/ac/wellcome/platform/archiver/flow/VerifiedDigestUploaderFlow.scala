package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Flow
import akka.{Done, NotUsed}
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation

object VerifiedDigestUploaderFlow {
  def apply(zipFile: ZipFile, bagName: BagName, config: BagUploaderConfig)(
    implicit
    materializer: ActorMaterializer,
    s3Client: S3Client
  ): Flow[ObjectLocation, Done, NotUsed] = {

    val bagDigestItemFlow
      : Flow[(ObjectLocation, BagName, ZipFile), BagDigestItem, NotUsed] =
      BagDigestItemFlow(config)
    val archiveItemFlow: Flow[(BagDigestItem, ZipFile), Done, NotUsed] =
      ArchiveItemFlow(config)

    Flow[ObjectLocation]
      .log("digest location")
      .map(location => (location, bagName, zipFile))
      .via(bagDigestItemFlow)
      .log("bag digest item")
      .map(bagDigestItem => (bagDigestItem, zipFile))
      .via(archiveItemFlow)
  }
}
