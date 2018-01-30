package uk.ac.wellcome.s3

import java.security.MessageDigest

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.{
  ItemIdentifier,
  Reindexable,
  Transformable,
  Versioned
}
import uk.ac.wellcome.test.utils.{JsonTestUtil, S3Local}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

case class TestObject(sourceId: String, sourceName: String, version: Int)
    extends Versioned

class SourceObjectStoreTest
    extends FunSpec
    with S3Local
    with Matchers
    with JsonTestUtil
    with ScalaFutures {

  val bucketName = createBucketAndReturnName("source-object-store")

  it("stores a versioned object with path id/version/hash") {

    val id = "b123"
    val version = 1
    val sourceName = "testSource"

    val objectStore = new SourceObjectStore(s3Client, bucketName)
    val testObject = TestObject(id, sourceName, version)

    val writtenToS3 = objectStore.put(testObject)

    whenReady(writtenToS3) { actualKey =>
      val expectedJson = JsonUtil.toJson(testObject).get
      val expectedHash = "75c6238d32ce27012cc919afa69512f0"

      val expectedKey = s"${sourceName}/$id/$version/$expectedHash.json"

      val jsonFromS3 = getJsonFromS3(
        bucketName,
        expectedKey
      ).noSpaces

      assertJsonStringsAreEqual(jsonFromS3, expectedJson)
      actualKey shouldBe expectedKey
    }
  }

  it("retrieves a versioned object from s3") {

    val id = "b789"
    val version = 2
    val sourceName = "anotherTestSource"

    val objectStore = new SourceObjectStore(s3Client, bucketName)
    val testObject = TestObject(id, sourceName, version)

    val writtenToS3 = objectStore.put(testObject)

    whenReady(writtenToS3.flatMap(objectStore.get[TestObject])) {
      actualTestObject =>
        actualTestObject shouldBe testObject
    }
  }

  it("throws an exception when retrieving a missing object") {
    val objectStore = new SourceObjectStore(s3Client, bucketName)

    whenReady(objectStore.get[TestObject]("not/a/real/object").failed) {
      exception =>
        exception shouldBe a[AmazonS3Exception]
        exception
          .asInstanceOf[AmazonS3Exception]
          .getErrorCode shouldBe "NoSuchKey"

    }
  }
}
