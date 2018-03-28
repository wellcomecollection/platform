package uk.ac.wellcome.platform.snapshot_convertor.source

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Compression, Sink, Source}
import akka.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.snapshot_convertor.fixtures.AkkaS3
import uk.ac.wellcome.test.fixtures.Akka

import scala.concurrent.Future

class S3SourceTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with Akka
    with AkkaS3 {

  it("reads a series of lines from S3") {
    withActorSystem { actorSystem =>
      implicit val system = actorSystem
      implicit val materializer = ActorMaterializer()

      withLocalS3Bucket { bucketName =>
        withS3AkkaClient(actorSystem, materializer) { akkaS3client =>
          val expectedLines = List(
            "AARGH! An armada of alpaccas",
            "Blimey! Blue bonobos with beachballs",
            "Cor! Countless copies of crying camels"
          )

          val key = "test001.txt.gz"

          whenReady(gzipContent(expectedLines.mkString("\n"))) { content =>
            s3Client.putObject(bucketName, key, content)
          }

          val source = S3Source(
            s3client = akkaS3client, bucketName = bucketName, key = key
          )

          val future = source.runWith(Sink.head)
          whenReady(future) { result =>
            result shouldBe expectedLines
          }
        }
      }
    }
  }

  private def gzipContent(content: String): Future[String] = {
    Source
      .single(content)
      .map { ByteString(_) }
      .via(Compression.gzip)
      .map { _.utf8String }
      .runWith(Sink.head)
  }
}
