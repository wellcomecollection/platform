package uk.ac.wellcome.platform.snapshot_convertor.source

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Compression, Sink, Source}
import akka.util.ByteString
import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.snapshot_convertor.fixtures.AkkaS3
import uk.ac.wellcome.platform.snapshot_convertor.test.utils.GzipUtils
import uk.ac.wellcome.test.fixtures.{Akka, S3}
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.Future

class S3SourceTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ExtendedPatience
    with Akka
    with S3
    with AkkaS3
    with GzipUtils {

  it("reads a series of lines from S3") {
    withActorSystem { actorSystem =>
      implicit val system = actorSystem
      implicit val materializer = ActorMaterializer()

      withLocalS3Bucket { bucket =>
        withS3AkkaClient(actorSystem, materializer) { akkaS3client =>
          val expectedLines = List(
            "AARGH! An armada of alpaccas",
            "Blimey! Blue bonobos with beachballs",
            "Cor! Countless copies of crying camels"
          )
          val content = expectedLines.mkString("\n")

          withGzipCompressedS3Key(bucket, content) { key =>
            val s3inputStream = s3Client
              .getObject(bucket.underlying, key)
              .getObjectContent()

            val source = S3Source(s3inputStream = s3inputStream)

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

      withLocalS3Bucket { bucket =>
        withS3AkkaClient(actorSystem, materializer) { akkaS3client =>
          val key = "example.txt.gz"
          s3Client.putObject(bucket.underlying, key, "NotAGzipCompressedFile")

          val s3inputStream = s3Client
            .getObject(bucket.underlying, key)
            .getObjectContent()

          val source = S3Source(s3inputStream = s3inputStream)

          val future = source.runWith(Sink.seq)
          whenReady(future.failed) { exception =>
            exception shouldBe a[RuntimeException]
          }
        }
      }
    }
  }
}
