package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.File
import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, StreamConverters}
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest

import scala.util.Success

object ZipFileDownloadFlow extends Logging {

  def apply()(implicit s3Client: AmazonS3)
  : Flow[IngestBagRequest,
    ZipFileDownloadComplete,
    NotUsed] = {

    Flow[IngestBagRequest]
      .log("download location")
      .flatMapConcat {
        case request@IngestBagRequest(_, location, _) =>

          val response = s3Client.getObject(location.namespace, location.key)
          val inputStream = response.getObjectContent

          val downloadStream = StreamConverters
            .fromInputStream(() => inputStream)

          val tmpFile = File.createTempFile("archivist", ".tmp")

          downloadStream
            .via(FileStoreFlow(tmpFile))
            .map(_.status)
            // TODO: Log failure here ?divertTo
            .collect {
            case Success(_) => ZipFileDownloadComplete(
              new ZipFile(tmpFile),
              request
            )
          }
      }.async
      .log("downloaded zipfile")
  }
}

case class ZipFileDownloadComplete(zipFile: ZipFile, ingestBagRequest: IngestBagRequest)
