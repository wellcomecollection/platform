package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.File
import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.Attributes
import akka.stream.scaladsl.{Flow, StreamConverters}
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.errors.{ArchiveError, ZipFileDownloadingError}
import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest

import scala.util.{Failure, Success}

object ZipFileDownloadFlow extends Logging {

  def apply(parallelism: Int)(implicit s3Client: AmazonS3)
    : Flow[IngestBagRequest, Either[ArchiveError[IngestBagRequest],ZipFileDownloadComplete], NotUsed] = {

    Flow[IngestBagRequest].withAttributes(Attributes.name(""))
      .log("download location")
      .flatMapMerge(
        parallelism, {
          case request @ IngestBagRequest(_, location, _) =>
            val response = s3Client.getObject(location.namespace, location.key)
            val inputStream = response.getObjectContent

            val downloadStream = StreamConverters
              .fromInputStream(() => inputStream)

            val tmpFile = File.createTempFile("archivist", ".tmp")

            downloadStream
              .via(FileStoreFlow(tmpFile, parallelism))
              .map {result => result.status match {
                case Success(_) =>
                  Right(ZipFileDownloadComplete(
                    new ZipFile(tmpFile),
                    request
                  ))
                case Failure(ex) =>
                  warn(s"Failed downloading zipFile from $location")
                  Left(ZipFileDownloadingError(request))
                  }
              }
        }
      )
      .async
      .log("downloaded zipfile")
  }
}

case class ZipFileDownloadComplete(zipFile: ZipFile,
                                   ingestBagRequest: IngestBagRequest)
