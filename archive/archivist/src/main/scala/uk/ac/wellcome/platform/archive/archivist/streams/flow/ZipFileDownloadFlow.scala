package uk.ac.wellcome.platform.archive.archivist.streams.flow

import java.io.File
import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.IngestRequestContext
import uk.ac.wellcome.storage.ObjectLocation

import scala.util.{Failure, Success}

object ZipFileDownloadFlow extends Logging {

  def apply()(implicit s3Client: S3Client)
  : Flow[(ObjectLocation, IngestRequestContext),
    (ZipFile, IngestRequestContext),
    NotUsed] = {

    Flow[(ObjectLocation, IngestRequestContext)]
      .log("download location")
      .flatMapConcat {
        case (objectLocation, ingestRequestContext) =>

          val (downloadStream, _) = s3Client.download(
            objectLocation.namespace,
            objectLocation.key
          )

          val tmpFile = File.createTempFile("archivist", ".tmp")

          downloadStream
            .via(FileStoreFlow(tmpFile))
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


