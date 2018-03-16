package uk.ac.wellcome.s3

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.test.utils.{ExtendedPatience, JsonTestUtil}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.fixtures.S3

import scala.concurrent.ExecutionContext.Implicits.global

case class TestObject(content: String)

class S3ObjectStoreTest
    extends FunSpec
    with S3
    with Matchers
    with JsonTestUtil
    with ScalaFutures
    with ExtendedPatience {

  it("stores a versioned object with path id/version/hash") {
    withLocalS3Bucket { bucketName =>
      val content = "Some content!"
      val prefix = "foo"

      val objectStore = new S3ObjectStore(
        s3Client,
        bucketName,
        new KeyPrefixGenerator[TestObject] {
          override def generate(obj: TestObject): String = prefix
        }
      )

      val testObject = TestObject(content = content)

      val writtenToS3 = objectStore.put(testObject)

      whenReady(writtenToS3) { actualKey =>
        val expectedJson = JsonUtil.toJson(testObject).get
        val expectedHash = "1770874231"

        val expectedKey = s"$prefix/$expectedHash.json"
        actualKey shouldBe expectedKey

        val jsonFromS3 = getJsonFromS3(
          bucketName,
          expectedKey
        ).noSpaces

        assertJsonStringsAreEqual(jsonFromS3, expectedJson)
      }
    }
  }

  it("removes leading slashes from prefixes") {
    withLocalS3Bucket { bucketName =>
      val content = "Some content!"
      val prefix = "/foo"

      val objectStore = new S3ObjectStore(
        s3Client,
        bucketName,
        new KeyPrefixGenerator[TestObject] {
          override def generate(obj: TestObject): String = prefix
        }
      )

      val testObject = TestObject(content = content)
      val writtenToS3 = objectStore.put(testObject)

      whenReady(writtenToS3) { actualKey =>
        val expectedHash = "1770874231"

        val expectedKey = s"foo/$expectedHash.json"
        actualKey shouldBe expectedKey
      }
    }
  }

  it("removes trailing slashes from prefixes") {
    withLocalS3Bucket { bucketName =>
      val content = "Some content!"
      val prefix = "foo/"

      val objectStore = new S3ObjectStore(
        s3Client,
        bucketName,
        new KeyPrefixGenerator[TestObject] {
          override def generate(obj: TestObject): String = prefix
        }
      )

      val testObject = TestObject(content = content)
      val writtenToS3 = objectStore.put(testObject)

      whenReady(writtenToS3) { actualKey =>
        val expectedHash = "1770874231"

        val expectedKey = s"foo/$expectedHash.json"
        actualKey shouldBe expectedKey
      }
    }
  }

  it("retrieves a versioned object from s3") {
    withLocalS3Bucket { bucketName =>
      val content = "Some content!"
      val prefix = "foo"

      val objectStore = new S3ObjectStore(
        s3Client,
        bucketName,
        new KeyPrefixGenerator[TestObject] {
          override def generate(obj: TestObject): String = prefix
        }
      )

      val testObject = TestObject(content = content)

      val writtenToS3 = objectStore.put(testObject)

      whenReady(writtenToS3.flatMap(objectStore.get)) { actualTestObject =>
        actualTestObject shouldBe testObject
      }
    }
  }

  it("throws an exception when retrieving a missing object") {
    withLocalS3Bucket { bucketName =>
      val objectStore = new S3ObjectStore(
        s3Client,
        bucketName,
        new KeyPrefixGenerator[TestObject] {
          override def generate(obj: TestObject): String = "doesnt_matter"
        }
      )

      whenReady(objectStore.get("not/a/real/object").failed) { exception =>
        exception shouldBe a[AmazonS3Exception]
        exception
          .asInstanceOf[AmazonS3Exception]
          .getErrorCode shouldBe "NoSuchKey"

      }
    }
  }
}
