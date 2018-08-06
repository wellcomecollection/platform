package uk.ac.wellcome.platform.archiver.flow

import java.io.File
import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{FileIO, Flow, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


object DownloadZipFlow extends Logging {
  val tmpDir = System.getProperty("java.io.tmpdir")

  def apply(s3Client: S3Client, materializer: ActorMaterializer, executionContext: ExecutionContext): Flow[ObjectLocation, ZipFile, NotUsed] = {
    implicit val m = materializer
    implicit val e = executionContext

    Flow[ObjectLocation]
      .log("download location")
      .flatMapConcat(objectLocation => {

        val (downloadStream, _) = s3Client.download(
          objectLocation.namespace,
          objectLocation.key
        )

        val tmpFile = File.createTempFile("archiver", ".tmp")
        val fileSink = FileIO.toPath(tmpFile.toPath)

        Source.fromFuture(downloadStream
          .runWith(fileSink)
          .map(_.status)
          .map {
            case Success(_) => new ZipFile(tmpFile)
            case Failure(e) => throw e
          }
        )
      }).log("downloaded zipfile")
  }
}


