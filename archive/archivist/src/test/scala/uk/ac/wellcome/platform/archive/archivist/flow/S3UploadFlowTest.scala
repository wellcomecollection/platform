package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.ByteArrayInputStream

import akka.stream.scaladsl.{Sink, Source, StreamConverters}
import akka.util.ByteString
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.test.fixtures.Akka

class S3UploadFlowTest extends FunSpec with S3 with ScalaFutures with Akka {

  it("writes a stream of bytestring to S3"){
      withActorSystem { implicit actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          withLocalS3Bucket { bucket =>
            val content = "dsrkjgherg"
            val s3Key = "key.txt"
            val futureResult = StreamConverters.fromInputStream(() => new ByteArrayInputStream(content.getBytes())).via(S3UploadFlow(ObjectLocation(bucket.name, s3Key))(s3Client)).runWith(Sink.head)

            whenReady(futureResult) { result =>
              result.get.getBucketName shouldBe bucket.name
              result.get.getKey shouldBe s3Key

              getContentFromS3(bucket, s3Key) shouldBe content
            }
          }
        }
      }
  }

  it("does not create an object in s3 if it doesn't receive an input stream"){
      withActorSystem { implicit actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          withLocalS3Bucket { bucket =>
            val s3Key = "key.txt"
            val futureResult = Source.empty.via(S3UploadFlow(ObjectLocation(bucket.name, s3Key))(s3Client)).runWith(Sink.seq)

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

}
