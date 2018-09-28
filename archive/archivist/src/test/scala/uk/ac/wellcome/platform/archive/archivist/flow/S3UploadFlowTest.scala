package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.ByteArrayInputStream

import akka.stream.scaladsl.{Concat, Sink, Source, StreamConverters}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.util.ByteString
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest
import org.apache.commons.io.IOUtils
import org.scalatest.FunSpec
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.test.fixtures.Akka

import scala.util.Failure

class S3UploadFlowTest extends FunSpec with S3 with ScalaFutures with Akka with PatienceConfiguration{

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(40, Seconds)),
    interval = scaled(Span(150, Millis))
  )

  it("writes a stream of bytestring to S3") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          val content = "dsrkjgherg"
          val s3Key = "key.txt"
          val futureResult = StreamConverters
            .fromInputStream(() => new ByteArrayInputStream(content.getBytes()))
            .via(S3UploadFlow(ObjectLocation(bucket.name, s3Key))(s3Client))
            .runWith(Sink.head)

          whenReady(futureResult) { triedResult =>
            triedResult.get.getBucketName shouldBe bucket.name
            triedResult.get.getKey shouldBe s3Key

            getContentFromS3(bucket, s3Key) shouldBe content
          }
        }
      }
    }
  }

  it("writes a stream from a jp2 to S3") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          val s3Key = "key.txt"
          val futureResult = StreamConverters
            .fromInputStream(() => getClass.getResourceAsStream("/b22454408_0001.jp2"))
            .via(S3UploadFlow(ObjectLocation(bucket.name, s3Key))(s3Client))
            .runWith(Sink.head)

          whenReady(futureResult) { triedResult =>
            triedResult.get.getBucketName shouldBe bucket.name
            triedResult.get.getKey shouldBe s3Key

            val actualObjectStream = s3Client.getObject(bucket.name, s3Key).getObjectContent
            val actualBytes = IOUtils.toByteArray(actualObjectStream)

            val expectedBytes = IOUtils.toByteArray(getClass.getResourceAsStream("/b22454408_0001.jp2"))
            actualBytes shouldBe expectedBytes
          }
        }
      }
    }
  }

  it("does not create an object in s3 if it doesn't receive an input stream") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          val s3Key = "key.txt"
          val futureResult = Source.empty
            .via(S3UploadFlow(ObjectLocation(bucket.name, s3Key))(s3Client))
            .runWith(Sink.seq)

          whenReady(futureResult) { result =>
            result shouldBe empty

            intercept[Exception] {
              getContentFromS3(bucket, s3Key)
            }
          }
        }
      }
    }
  }

  it("uploads a big bytestring of random bytes") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          val res = Array.fill(23 * 1024 * 1024)(
            (scala.util.Random.nextInt(256) - 128).toByte)
          val s3Key = "key.txt"
          val futureResult = Source
            .single(ByteString(res))
            .via(S3UploadFlow(ObjectLocation(bucket.name, s3Key))(s3Client))
            .runWith(Sink.seq)

          whenReady(futureResult) { result =>
            result should have size 1
            result.head.get.getBucketName shouldBe bucket.name
            result.head.get.getKey shouldBe s3Key

            val is = s3Client.getObject(bucket.name, s3Key).getObjectContent
            Stream
              .continually(is.read)
              .takeWhile(_ != -1)
              .map(_.toByte)
              .toArray shouldBe res
          }
        }
      }
    }
  }

  it("aborts the upload request if the stream fails elsewhere") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          val s3Key = "key.txt"
          val expectedException = new RuntimeException("BOOM!")
          val futureResult = Source
            .combine(
              Source.single(ByteString("abcdesf", "UTF-8")),
              Source.failed(expectedException))(Concat(_))
            .via(S3UploadFlow(ObjectLocation(bucket.name, s3Key))(s3Client))
            .runWith(Sink.seq)

          whenReady(futureResult.failed) { result =>
            result shouldBe expectedException

            val multipartUploadListing = s3Client.listMultipartUploads(
              new ListMultipartUploadsRequest(bucket.name))
            multipartUploadListing.getMultipartUploads shouldBe empty
          }
        }
      }
    }
  }

  it("respects the supervision strategy on upstream failure") {
    withActorSystem { implicit actorSystem =>
      val decider: Supervision.Decider = { e =>
        error("Stream failure", e)
        Supervision.Resume
      }
      implicit val materializer = ActorMaterializer(
        ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider)
      )
      withLocalS3Bucket { bucket =>
        val s3Key = "key.txt"
        val expectedException = new RuntimeException("BOOM!")
        val futureResult = Source
          .combine(
            Source.single(ByteString("abcdesf", "UTF-8")),
            Source.failed(expectedException),
            Source.single(ByteString("abcdesf", "UTF-8")))(Concat(_))
          .via(S3UploadFlow(ObjectLocation(bucket.name, s3Key))(s3Client))
          .runWith(Sink.seq)

        whenReady(futureResult) { result =>
          result shouldBe empty
        }
      }
    }
  }

  it("respects the supervision strategy on internal failure") {
    withActorSystem { implicit actorSystem =>
      val decider: Supervision.Decider = { e =>
        error("Stream failure", e)
        Supervision.Resume
      }
      implicit val materializer = ActorMaterializer(
        ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider)
      )
      val s3Key = "key.txt"
      val futureResult = Source
        .single(ByteString("abcdesf", "UTF-8"))
        .via(S3UploadFlow(ObjectLocation("does-not-exist", s3Key))(s3Client))
        .runWith(Sink.seq)

      whenReady(futureResult) { result =>
        result should have size 1
        result.head shouldBe a[Failure[_]]
      }
    }
  }

}
