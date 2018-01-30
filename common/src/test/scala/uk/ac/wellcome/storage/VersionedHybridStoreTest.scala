package uk.ac.wellcome.storage

import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.syntax._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.locals.DynamoDBLocal
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{VersionUpdater, Versioned}
import uk.ac.wellcome.s3.VersionedObjectStore
import uk.ac.wellcome.test.utils.{ExtendedPatience, JsonTestUtil, S3Local}
import uk.ac.wellcome.utils.JsonUtil._


case class ExampleRecord(
  version: Int,
  sourceId: String,
  sourceName: String,
  content: String
) extends Versioned


class VersionedHybridStoreTest extends FunSpec with Matchers with S3Local with ScalaFutures with DynamoDBLocal[ExampleRecord] with JsonTestUtil with ExtendedPatience {

  implicit val testVersionUpdater = new VersionUpdater[ExampleRecord]{
    override def updateVersion(testVersioned: ExampleRecord, newVersion: Int): ExampleRecord = {
      testVersioned.copy(version = newVersion)
    }
  }

  override lazy val evidence: DynamoFormat[ExampleRecord] = DynamoFormat[ExampleRecord]

  override lazy val tableName: String = "versioned-hybrid-store-test"
  val bucketName = createBucketAndReturnName("versioned-hybrid-store-test")

  val hybridStore = new VersionedHybridStore(
    versionedObjectStore = new VersionedObjectStore(s3Client = s3Client, bucketName = bucketName),
    versionedDao = new VersionedDao(dynamoDbClient = dynamoDbClient, dynamoConfig = DynamoConfig(
      table = tableName
    ))
  )

  it("stores a versioned record if it has never been seen before") {
    val record = ExampleRecord(
      version = 1,
      sourceId = "1111",
      sourceName = "Test1111",
      content = "One ocelot in orange"
    )

    val future = hybridStore.updateRecord(record)

    whenReady(future) { _ =>
      val dynamoRecord = Scanamo.get[HybridRecord](dynamoDbClient)(tableName)('id -> record.id).get
      dynamoRecord.isRight shouldBe true
      val hybridRecord = dynamoRecord.right.get

      hybridRecord.version shouldBe (record.version + 1)
      hybridRecord.sourceId shouldBe record.sourceId
      hybridRecord.sourceName shouldBe record.sourceName

      val s3key = hybridRecord.s3key

      val retrievedJson = getJsonFromS3(bucketName = bucketName, key = s3key).noSpaces
      assertJsonStringsAreEqual(retrievedJson, toJson(record.copy(version = 2)).get)
    }
  }

}
