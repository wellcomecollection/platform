package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Flow, GraphDSL, Source, Zip}
import akka.stream.{FlowShape, SourceShape}
import akka.util.ByteString
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.{BagDigestItem, BagLocation}
import uk.ac.wellcome.storage.ObjectLocation

import scala.util.{Failure, Success, Try}

trait CompareChecksum extends Logging {
  def compare[T](checksum: String): PartialFunction[(T, ByteString), Try[T]] = {
    case (result, byteChecksum: ByteString) => Try {

      val calculatedChecksum = byteChecksum
        .map(0xFF & _)
        .map("%02x".format(_))
        .foldLeft("") {
          _ + _
        }
        .mkString

      if (calculatedChecksum != checksum) {
        throw new RuntimeException(
          s"Bad checksum! ($calculatedChecksum != $checksum"
        )
      } else {
        debug(s"Checksum match! ($calculatedChecksum != $checksum")
      }

      result
    }
  }
}

object UploadVerificationFlow
  extends Logging
    with CompareChecksum {
  def apply()(
    implicit s3Client: S3Client
  ): Flow[ArchiveItemJob, Either[ArchiveItemJob, ArchiveItemJob],
    NotUsed] = {

    Flow[ArchiveItemJob]
      .flatMapConcat {
        case job@ArchiveItemJob(archiveJob, BagDigestItem(checksum, itemLocation)) =>
          val extract = FileExtractorFlow()
          val verify = DigestCalculatorFlow("SHA-256")
          val source: Source[(ObjectLocation, ZipFile), NotUsed] =
            Source.single((itemLocation, archiveJob.zipFile))

          val upload = createS3UploadFlow(s3Client, job.uploadLocation)

          val checkedUpload = Source.fromGraph(
            GraphDSL.create(source, extract, upload, verify)((_, _, _, _)) {
              implicit b =>
                (s, e, u, v) => {

                  import GraphDSL.Implicits._

                  val zip = b.add(Zip[MultipartUploadResult, ByteString])

                  s ~> e ~> v.inlets.head

                  v.outlets(0) ~> u ~> zip.in0
                  v.outlets(1) ~> zip.in1

                  SourceShape(zip.out)
                }
            })

          checkedUpload
            .map(compare(checksum))
            .map {
              case Success(_) => Right(job)
              case Failure(_) => Left(job)
            }
      }
  }

  private def createS3UploadFlow(s3Client: S3Client, objectLocation: ObjectLocation) = {
    val s3Sink = s3Client.multipartUpload(
      objectLocation.namespace,
      objectLocation.key
    )

    Flow.fromGraph(GraphDSL.create(s3Sink) { implicit builder =>
      sink => FlowShape(sink.in, builder.materializedValue)
    }).flatMapConcat(Source.fromFuture)
  }
}
