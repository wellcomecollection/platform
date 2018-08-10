package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, Source}
import akka.{Done, NotUsed}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation

// TODO: Verify checksums in S3 are what you set them to
object  UploadAndVerifyBagFlow extends Logging {
  def apply(config: BagUploaderConfig)(
    implicit
    materializer: ActorMaterializer,
    s3Client: S3Client
  ): Flow[ZipFile, Done, NotUsed] = {

    Flow[ZipFile].flatMapConcat(zipFile => {
      Source
        .single(zipFile)
        .mapConcat(bagNames)
        .mapConcat(digestNames(_,config.digestNames))
        .flatMapConcat(digestLocation => {

          val bagName = BagName(digestLocation.namespace)

          val bagDigestItemFlow = BagDigestItemFlow(config)
          val archiveItemFlow = ArchiveItemFlow(config)

          Source
            .single(digestLocation)
            .log("digest location")
            .map(location => (location, bagName, zipFile))
            .via(bagDigestItemFlow)
            .log("bag digest item")
            .map(bagDigestItem => (bagDigestItem, zipFile))
            .via(archiveItemFlow)
        })
    })
  }

  private def digestNames(bagName: BagName, digestNames: List[String]) =
    digestNames.map(digestName => {
      ObjectLocation(bagName.value, digestName)
    })

  private def bagNames(zipFile: ZipFile) = {
    val entries = zipFile.entries()

    Stream
      .continually(entries.nextElement)
      .map(_.getName.split("/"))
      .filter(_.length > 1)
      .flatMap(_.headOption)
      .takeWhile(_ => entries.hasMoreElements)
      .toSet
      .filterNot(_.startsWith("_"))
      .map(BagName)
  }
}

case class BagName(value: String)
