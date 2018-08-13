package uk.ac.wellcome.platform.archiver

import java.util.zip.ZipFile

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archiver.flow.DownloadZipFileFlow
import uk.ac.wellcome.storage.ObjectLocation

import scala.collection.JavaConverters._
import scala.concurrent.Future

class DownloadZipFileFlowTest
  extends FunSpec
    with Matchers
    with ScalaFutures
    with fixtures.Archiver {

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()

  it("downloads a zipfile from s3") {

    withLocalS3Bucket { storageBucket =>
      withS3AkkaClient(system, materializer) { s3AkkaClient =>
        withBag() { case (bagName, zipFile, file) =>

          implicit val _ = s3AkkaClient

          val downloadZipFlow = DownloadZipFileFlow()(
            s3AkkaClient,
            materializer
          )

          val uploadKey = bagName.toString

          s3Client.putObject(storageBucket.name, uploadKey, file)

          val objectLocation = ObjectLocation(storageBucket.name, uploadKey)

          val download: Future[ZipFile] =
            downloadZipFlow.runWith(Source.single(objectLocation), Sink.head)._2

          whenReady(download) { downloadedZipFile =>
            zipFile.entries.asScala.toList
              .map(_.toString) should contain theSameElementsAs downloadedZipFile.entries.asScala.toList
              .map(_.toString)
            zipFile.size shouldEqual downloadedZipFile.size
          }
        }
      }
    }
  }
}
