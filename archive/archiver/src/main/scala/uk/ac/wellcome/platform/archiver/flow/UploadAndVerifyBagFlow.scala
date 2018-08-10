package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

// TODO: Verify checksums in S3 are what you set them to
object UploadAndVerifyBagFlow extends Logging {
  def apply(config: BagUploaderConfig)(
    implicit
    materializer: ActorMaterializer,
    s3Client: S3Client,
    executionContext: ExecutionContext
  ): Flow[ZipFile, BagLocation, NotUsed] = {

    Flow[ZipFile].flatMapConcat(zipFile => {
      Source
        .single(zipFile)
        .mapConcat(bagNames)
        .map(bagName => (bagName, createBagLocation(bagName, config)))
        .flatMapConcat { case (bagName, bagLocation) =>

          Source.fromFuture(
            ArchiveBagFlow(zipFile, bagLocation, config)
              .map(Success(_))
              .recover({ case e => Failure(e) })
              .toMat(Sink.seq)(Keep.right)
              .run()
              .map {
                case s if s.collect({ case a: Failure[_] => a }).nonEmpty =>
                  throw new RuntimeException("Failed!")
                case s => bagLocation
              })

        }
    })
  }

  private def createBagLocation(bagName: BagName, config: BagUploaderConfig) = {
    BagLocation(
      storageNamespace = config.uploadNamespace,
      storagePath = config.uploadPrefix,
      bagName = bagName
    )
  }

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