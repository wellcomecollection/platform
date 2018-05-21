package uk.ac.wellcome.storage.s3

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.ExecutionContext.Implicits.global

class S3StringStoreTest
    extends FunSpec
    with S3
    with Matchers
    with ScalaFutures
    with ExtendedPatience {

  val content = "Some content!"
  val expectedHash = "1227282840"

  it("stores a string with path id/version/hash") {
    withLocalS3Bucket { bucket =>
      withS3StringObjectStore(bucket) { stringStore =>
        val prefix = "foo"

        val writtenToS3 =
          stringStore.put(bucket.name)(content = content, keyPrefix = prefix)

        whenReady(writtenToS3) { actualKey =>
          val expectedKey = s"$prefix/$expectedHash"
          val expectedUri = S3ObjectLocation(bucket.name, expectedKey)

          actualKey shouldBe expectedUri

          val contentFromS3 = getContentFromS3(bucket, expectedKey)
          contentFromS3 shouldBe content
        }
      }
    }
  }

  it("retrieves a string from s3") {
    withLocalS3Bucket { bucket =>
      withS3StringObjectStore(bucket) { stringStore =>
        val prefix = "foo"

        val writtenToS3 =
          stringStore.put(bucket.name)(content = content, keyPrefix = prefix)

        whenReady(writtenToS3.flatMap(stringStore.get)) { actualContent =>
          actualContent shouldBe content
        }
      }
    }
  }

  it("throws an exception when retrieving a missing key") {
    withLocalS3Bucket { bucket =>
      withS3StringObjectStore(bucket) { stringStore =>
        whenReady(
          stringStore
            .get(S3ObjectLocation(bucket.name, "not/a/real/object"))
            .failed) { exception =>
          exception shouldBe a[AmazonS3Exception]
          exception
            .asInstanceOf[AmazonS3Exception]
            .getErrorCode shouldBe "NoSuchKey"

        }
      }
    }
  }

  private def withS3StringObjectStore(bucket: Bucket)(
    testWith: TestWith[S3StringStore, Assertion]) = {
    val stringStore = new S3StringStore(
      s3Client = s3Client
    )

    testWith(stringStore)
  }
}
