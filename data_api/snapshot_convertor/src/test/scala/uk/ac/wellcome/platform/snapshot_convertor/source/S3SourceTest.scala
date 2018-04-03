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
          val content = expectedLines.mkString("\n")

          withGzipCompressedS3Key(bucketName, content) { key =>
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

  // This test is meant to catch an error we saw when we first turned on
  // the snapshot convertor:
  //
  //    akka.http.scaladsl.model.EntityStreamSizeException:
  //    EntityStreamSizeException: actual entity size (Some(19403836)) exceeded
  //    content length limit (8388608 bytes)! You can configure this by setting
  //    `akka.http.[server|client].parsing.max-content-length` or calling
  //    `HttpEntity.withSizeLimit` before materializing the dataBytes stream.
  //
  // With the original code, we were unable to read anything more than
  // an 8MB file from S3.  This test deliberately creates a very large file,
  // and tries to stream it back out.
  //
  it("reads files that are larger than 8MB from S3") {
    withActorSystem { actorSystem =>
      implicit val system = actorSystem
      implicit val materializer = ActorMaterializer()

      withLocalS3Bucket { bucketName =>
        withS3AkkaClient(actorSystem, materializer) { akkaS3client =>

          // The aim here is to increase entropy in the source file, as a
          // way to increase the gzip-compressed size.
          val expectedLines = (1 to 1000000).map {
            idx => s"${idx} ${100 - idx} ${1000 - idx} ${10000 - idx} ${100000 - idx}"
          }

          val content = expectedLines.mkString("\n")

          withGzipCompressedS3Key(bucketName, content) { key =>
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
