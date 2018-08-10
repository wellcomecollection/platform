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
          val bagLocation = createBagLocation(bagName, config)

          val bagDigestItemFlow = BagDigestItemFlow(config.digestDelimiterRegexp)
          val archiveItemFlow = ArchiveItemFlow()

          Source
            .single(digestLocation)
            .log("digest location")
            .map((_, bagName, zipFile))
            .via(bagDigestItemFlow)
            .log("bag digest item")
            .map((bagLocation, _, zipFile))
            .via(archiveItemFlow)
        })
    })
  }

  private def createBagLocation(bagName: BagName, config: BagUploaderConfig) = {
    BagLocation(
      storageNamespace = config.uploadNamespace,
      storagePath = config.uploadPrefix,
      bagName = bagName
    )
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
