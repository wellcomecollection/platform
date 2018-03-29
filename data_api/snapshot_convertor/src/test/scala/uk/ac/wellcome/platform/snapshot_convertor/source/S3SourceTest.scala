package uk.ac.wellcome.platform.snapshot_convertor.source

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Compression, Sink, Source}
import akka.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.snapshot_convertor.fixtures.AkkaS3
import uk.ac.wellcome.platform.snapshot_convertor.test.utils.GzipUtils
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.Future

class S3SourceTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ExtendedPatience
    with Akka
    with AkkaS3
    with GzipUtils {

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

          withGzipCompressedS3Key(expectedLines.mkString("\n")) { key =>
            val source = S3Source(
              s3client = akkaS3client,
              bucketName = bucketName,
              key = key
            )

            val future = source.runWith(Sink.seq)
            whenReady(future) { result =>
              result.toList shouldBe expectedLines
            }
          }
        }
      }
    }
  }

  it("returns a failed future if it the S3 file is not gzip-compressed") {
    withActorSystem { actorSystem =>
      implicit val system = actorSystem
      implicit val materializer = ActorMaterializer()

      withLocalS3Bucket { bucketName =>
        withS3AkkaClient(actorSystem, materializer) { akkaS3client =>
          val key = "example.txt.gz"
          s3Client.putObject(bucketName, key, "NotAGzipCompressedFile")

          val source = S3Source(
            s3client = akkaS3client,
            bucketName = bucketName,
            key = key
          )

          val future = source.runWith(Sink.seq)
          whenReady(future.failed) { exception =>
            exception shouldBe a[RuntimeException]
          }
        }
      }
    }
  }

  it("returns a failed future if asked to fetch a non-existent file") {
    withActorSystem { actorSystem =>
      implicit val system = actorSystem
      implicit val materializer = ActorMaterializer()

      withLocalS3Bucket { bucketName =>
        withS3AkkaClient(actorSystem, materializer) { akkaS3client =>
          val source = S3Source(
            s3client = akkaS3client,
            bucketName = bucketName,
            key = "doesnotexist.txt.gz"
          )

          val future = source.runWith(Sink.seq)
          whenReady(future.failed) { exception =>
            exception shouldBe a[RuntimeException]
          }
        }
      }
    }
  }
}
