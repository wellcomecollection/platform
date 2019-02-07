package uk.ac.wellcome.platform.archive.bagreplicator.storage

import com.amazonaws.services.s3.model.PutObjectResult
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

class S3CopierTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with RandomThings
    with S3 {
  it("copies a file inside a bucket") {
    withLocalS3Bucket { bucket =>
      val src = ObjectLocation(
        namespace = bucket.name,
        key = "123.txt"
      )

      val dst = ObjectLocation(
        namespace = bucket.name,
        key = "456.txt"
      )

      createObject(src)
      listKeysInBucket(bucket) shouldBe List(src.key)

      withS3Copier { s3Copier =>
        val future = s3Copier.copy(src = src, dst = dst)

        whenReady(future) { _ =>
          listKeysInBucket(bucket) shouldBe List(src.key, dst.key)
          assertEqualObjects(src, dst)
        }
      }
    }
  }

  it("copies a file across different buckets") {
    withLocalS3Bucket { srcBucket =>
      val src = ObjectLocation(
        namespace = srcBucket.name,
        key = "123.txt"
      )

      withLocalS3Bucket { dstBucket =>
        val dst = ObjectLocation(
          namespace = dstBucket.name,
          key = "456.txt"
        )

        createObject(src)
        listKeysInBucket(srcBucket) shouldBe List(src.key)
        listKeysInBucket(dstBucket) shouldBe List()

        withS3Copier { s3Copier =>
          val future = s3Copier.copy(src = src, dst = dst)

          whenReady(future) { _ =>
            listKeysInBucket(srcBucket) shouldBe List(src.key)
            listKeysInBucket(dstBucket) shouldBe List(dst.key)
            assertEqualObjects(src, dst)
          }
        }
      }
    }
  }

  private def withS3Copier[R](testWith: TestWith[S3Copier, R]): R = {
    val s3Copier = new S3Copier(s3Client)
    testWith(s3Copier)
  }

  private def createObject(location: ObjectLocation): PutObjectResult =
    s3Client.putObject(
      location.namespace,
      location.key,
      randomAlphanumeric()
    )

  private def assertEqualObjects(x: ObjectLocation,
                                 y: ObjectLocation): Assertion =
    getContentFromS3(x) shouldBe getContentFromS3(y)
}
