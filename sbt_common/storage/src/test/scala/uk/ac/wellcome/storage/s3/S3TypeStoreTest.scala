package uk.ac.wellcome.storage.s3

import java.io.{ByteArrayInputStream, InputStream}

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.storage.{KeyPrefix, KeySuffix, ObjectLocation}
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.storage.type_classes.{StorageKey, StorageStrategy, StorageStream}
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.{ExtendedPatience, JsonTestUtil}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

case class TestObject(content: String)

class S3TypeStoreTest
    extends FunSpec
    with S3
    with Matchers
    with JsonTestUtil
    with ScalaFutures
    with ExtendedPatience {

  val content = "Some content!"
  val expectedHash = "698d6577"

  implicit val store: StorageStrategy[TestObject] =
    new StorageStrategy[TestObject] {
      def store(t: TestObject): StorageStream = {
        val key = StorageKey("key")
        val input = new ByteArrayInputStream(toJson(t).get.getBytes)

        StorageStream(input, key)
      }

      def retrieve(input: InputStream) =
        fromJson[TestObject](Source.fromInputStream(input).mkString)

    }

  it("stores a versioned object with path id/version/hash") {
    withLocalS3Bucket { bucket =>
      withS3TypeStore(bucket) { objectStore =>
        val content = "Some content!"
        val prefix = KeyPrefix("foo")
        val suffix = KeySuffix(".json")

        val testObject = TestObject(content = content)

        val writtenToS3 = objectStore.put(bucket.name)(testObject, prefix, suffix)

        whenReady(writtenToS3) { actualKey =>
          val expectedJson = JsonUtil.toJson(testObject).get

          val expectedKey = s"${prefix.value}/$expectedHash.json"
          val expectedUri = ObjectLocation(bucket.name, expectedKey)

          actualKey shouldBe expectedUri

          val jsonFromS3 = getJsonFromS3(
            bucket,
            expectedKey
          ).noSpaces

          assertJsonStringsAreEqual(jsonFromS3, expectedJson)
        }
      }
    }
  }

  it("removes leading slashes from prefixes") {
    withLocalS3Bucket { bucket =>
      withS3TypeStore(bucket) { objectStore =>
        val prefix = KeyPrefix("foo")
        val suffix = KeySuffix(".json")

        val testObject = TestObject(content = content)
        val writtenToS3 = objectStore.put(bucket.name)(testObject, prefix, suffix)

        whenReady(writtenToS3) { actualKey =>
          val expectedUri =
            ObjectLocation(bucket.name, s"foo/$expectedHash.json")
          actualKey shouldBe expectedUri
        }
      }
    }
  }

  it("removes trailing slashes from prefixes") {
    withLocalS3Bucket { bucket =>
      withS3TypeStore(bucket) { objectStore =>
        val prefix = KeyPrefix("foo/")
        val suffix = KeySuffix(".json")

        val testObject = TestObject(content = content)
        val writtenToS3 = objectStore.put(bucket.name)(testObject, prefix, suffix)

        whenReady(writtenToS3) { actualKey =>
          val expectedUri =
            ObjectLocation(bucket.name, s"foo/$expectedHash.json")
          actualKey shouldBe expectedUri
        }
      }
    }
  }

  it("retrieves a versioned object from s3") {
    withLocalS3Bucket { bucket =>
      withS3TypeStore(bucket) { objectStore =>
        val prefix = KeyPrefix("foo")
        val suffix = KeySuffix(".json")

        val testObject = TestObject(content = content)

        val writtenToS3 = objectStore.put(bucket.name)(testObject, prefix, suffix)

        whenReady(writtenToS3.flatMap(objectStore.get)) { actualTestObject =>
          actualTestObject shouldBe testObject
        }
      }
    }
  }

  it("throws an exception when retrieving a missing object") {
    withLocalS3Bucket { bucket =>
      withS3TypeStore(bucket) { objectStore =>
        whenReady(
          objectStore
            .get(ObjectLocation(bucket.name, "not/a/real/object"))
            .failed) { exception =>
          exception shouldBe a[AmazonS3Exception]
          exception
            .asInstanceOf[AmazonS3Exception]
            .getErrorCode shouldBe "NoSuchKey"

        }
      }
    }
  }

  private def withS3TypeStore(bucket: Bucket)(
    testWith: TestWith[S3TypeStore[TestObject], Assertion]) = {
    val s3TypeStore = new S3TypeStore[TestObject](
      new S3StorageBackend(s3Client)
    )

    testWith(s3TypeStore)
  }
}
