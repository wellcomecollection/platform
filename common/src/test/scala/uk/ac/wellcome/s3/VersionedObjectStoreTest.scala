package uk.ac.wellcome.s3

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{VersionUpdater, Versioned}
import uk.ac.wellcome.test.utils.{JsonTestUtil, S3Local}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

case class TestObject(sourceId: String, sourceName: String, version: Int)
    extends Versioned

class VersionedObjectStoreTest
    extends FunSpec
    with S3Local
    with Matchers
    with JsonTestUtil
    with ScalaFutures {

  lazy val bucketName = "source-object-store"

  implicit val testVersionUpdater = new VersionUpdater[TestObject] {
    override def updateVersion(testVersioned: TestObject,
                               newVersion: Int): TestObject = {
      testVersioned.copy(version = newVersion)
    }
  }

  it("stores a versioned object with path id/version/hash") {

    val id = "b123"
    val version = 1
    val sourceName = "testSource"

    val objectStore = new VersionedObjectStore(s3Client, bucketName)
    val testObject = TestObject(id, sourceName, version)

    val writtenToS3 = objectStore.put(testObject)

    whenReady(writtenToS3) { actualKey =>
      val expectedJson = JsonUtil.toJson(testObject.copy(version = 2)).get
      val expectedHash = "974513547"

      val expectedKey = s"${sourceName}/$id/${version + 1}/$expectedHash.json"
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

    val objectStore = new VersionedObjectStore(s3Client, bucketName)
    val testObject = TestObject(id, sourceName, version)

    val writtenToS3 = objectStore.put(testObject)

    whenReady(writtenToS3.flatMap(objectStore.get[TestObject])) {
      actualTestObject =>
        val expectedObject = testObject.copy(version = testObject.version + 1)
        actualTestObject shouldBe expectedObject
    }
  }

  it("throws an exception when retrieving a missing object") {
    val objectStore = new VersionedObjectStore(s3Client, bucketName)

    whenReady(objectStore.get[TestObject]("not/a/real/object").failed) {
      exception =>
        exception shouldBe a[AmazonS3Exception]
        exception
          .asInstanceOf[AmazonS3Exception]
          .getErrorCode shouldBe "NoSuchKey"

    }
  }
}
