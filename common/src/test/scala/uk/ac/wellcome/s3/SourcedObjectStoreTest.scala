package uk.ac.wellcome.s3

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.test.utils.{ExtendedPatience, JsonTestUtil, S3Local}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

case class TestObject(sourceId: String, sourceName: String)
    extends Sourced

class SourcedObjectStoreTest
    extends FunSpec
    with S3Local
    with Matchers
    with JsonTestUtil
    with ScalaFutures
    with ExtendedPatience {

  lazy val bucketName = "source-object-store"

  it("stores a versioned object with path id/version/hash") {

    val id = "b123"
    val version = 1
    val sourceName = "testSource"

    val objectStore = new SourcedObjectStore(s3Client, bucketName)
    val testObject = TestObject(sourceId = id, sourceName = sourceName)

    val writtenToS3 = objectStore.put(testObject)

    whenReady(writtenToS3) { actualKey =>
      val expectedJson = JsonUtil.toJson(testObject).get
      val expectedHash = "112520602"

      val expectedKey = s"${sourceName}/$id/32/$expectedHash.json"
      actualKey shouldBe expectedKey

      val jsonFromS3 = getJsonFromS3(
        bucketName,
        expectedKey
      ).noSpaces

      assertJsonStringsAreEqual(jsonFromS3, expectedJson)
    }
  }

  it("retrieves a versioned object from s3") {

    val id = "b789"
    val version = 2
    val sourceName = "anotherTestSource"

    val objectStore = new SourcedObjectStore(s3Client, bucketName)
    val testObject = TestObject(sourceId = id, sourceName = sourceName)

    val writtenToS3 = objectStore.put(testObject)

    whenReady(writtenToS3.flatMap(objectStore.get[TestObject])) {
      actualTestObject => actualTestObject shouldBe testObject
    }
  }

  it("throws an exception when retrieving a missing object") {
    val objectStore = new SourcedObjectStore(s3Client, bucketName)

    whenReady(objectStore.get[TestObject]("not/a/real/object").failed) {
      exception =>
        exception shouldBe a[AmazonS3Exception]
        exception
          .asInstanceOf[AmazonS3Exception]
          .getErrorCode shouldBe "NoSuchKey"

    }
  }
}
