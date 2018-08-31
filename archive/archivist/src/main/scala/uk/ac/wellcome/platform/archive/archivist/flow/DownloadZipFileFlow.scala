package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.File
import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{FileIO, Flow, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.IngestRequestContext
import uk.ac.wellcome.storage.ObjectLocation

import scala.util.{Failure, Success}

object DownloadZipFileFlow extends Logging {
  def apply()(
    implicit s3Client: S3Client,
    materializer: ActorMaterializer): Flow[(ObjectLocation, IngestRequestContext), (ZipFile, IngestRequestContext), NotUsed] = {

    Flow[(ObjectLocation, IngestRequestContext)]
      .log("download location")
      .flatMapConcat {
        case (objectLocation, ingestRequestContext) =>

        val (downloadStream, _) = s3Client.download(
          objectLocation.namespace,
          objectLocation.key
        )

        val tmpFile = File.createTempFile("archivist", ".tmp")
        val fileSink = FileIO.toPath(tmpFile.toPath)

        Source
          .fromFuture(downloadStream.runWith(fileSink))
          .map(_.status)
          .map {
            case Success(_) =>
              (new ZipFile(tmpFile), ingestRequestContext)
            case Failure(e) =>
              throw e
          }
      }
      .log("downloaded zipfile")
  }
}
