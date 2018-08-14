package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{BagUploaderConfig, UploadConfig}

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
        .map(bagName =>
          (bagName, createBagLocation(bagName, config.uploadConfig)))
        .map {
          case (bagName, bagLocation) =>
            materializeArchiveBagFlow(zipFile, bagLocation, config)
        }
        .flatMapConcat(Source.fromFuture)
    })
  }

  private def materializeArchiveBagFlow(
                                         zipFile: ZipFile,
                                         bagLocation: BagLocation,
                                         config: BagUploaderConfig
                                       )(
                                         implicit
                                         materializer: ActorMaterializer,
                                         s3Client: S3Client,
                                         executionContext: ExecutionContext
                                       ) =
    ArchiveBagFlow(zipFile, bagLocation, config.bagItConfig)
      .map(Success(_))
      .recover({ case e => Failure(e) })
      .toMat(Sink.seq)(Keep.right)
      .run()
      .map(_.collect { case Failure(e) => e })
      .collect {
        case failureList if failureList.nonEmpty => {
          throw new FailedArchivingException(bagLocation.bagName, failureList)
        }
        case _ => bagLocation
      }

  private def createBagLocation(bagName: BagName, config: UploadConfig) = {
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

case class FailedArchivingException(bagName: BagName, e: Seq[Throwable])
  extends RuntimeException(
    s"Failed archiving: $bagName:\n${e.map(_.getMessage).mkString}"
  ) {}

case class BagName(value: String) {
  override def toString: String = value
}
