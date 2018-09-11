package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.File
import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{FileIO, Flow, GraphDSL, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.IngestRequestContext
import uk.ac.wellcome.storage.ObjectLocation

import scala.util.{Failure, Success}

object DownloadZipFileFlow extends Logging {
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
            .via(fileStoreFlow(tmpFile))
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

  private def fileStoreFlow(tmpFile: File) = {
    val fileSink = FileIO.toPath(tmpFile.toPath)

    Flow.fromGraph(GraphDSL.create(fileSink) { implicit builder =>
      sink =>
        FlowShape(sink.in, builder.materializedValue)
    }).flatMapConcat(Source.fromFuture)
  }
}
