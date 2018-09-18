package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.File
import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest

import scala.util.Success

object ZipFileDownloadFlow extends Logging {

  def apply()(implicit s3Client: S3Client)
  : Flow[IngestBagRequest,
    ZipFileDownloadComplete,
    NotUsed] = {

    Flow[IngestBagRequest]
      .log("download location")
      .flatMapConcat {
        case request@IngestBagRequest(_, location, _) =>

          val (downloadStream, _) = s3Client.download(
            location.namespace,
            location.key
          )

          val tmpFile = File.createTempFile("archivist", ".tmp")

          downloadStream
            .via(FileStoreFlow(tmpFile))
            .map(_.status)
            // TODO: Log failure here
            .collect {
            case Success(_) => ZipFileDownloadComplete(
              new ZipFile(tmpFile),
              request
            )
          }
      }
      .log("downloaded zipfile")
  }
}

case class ZipFileDownloadComplete(zipFile: ZipFile, ingestBagRequest: IngestBagRequest)
