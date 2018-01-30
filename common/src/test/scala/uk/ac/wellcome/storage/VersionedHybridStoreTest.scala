package uk.ac.wellcome.storage

import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
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
import uk.ac.wellcome.utils.GlobalExecutionContext._


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
  val bucketName = "versioned-hybrid-store-test"

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
      assertJsonStringsAreEqual(retrievedJson, toJson(record.copy(version = record.version + 1)).get)
    }
  }

  it("updates DynamoDB and S3 if it sees a new version of a record") {
    val record = ExampleRecord(
      version = 2,
      sourceId = "2222",
      sourceName = "Test2222",
      content = "Two teal turtles in Tenerife"
    )

    val updatedRecord = record.copy(
      version = 3,
      content = "Throwing turquoise tangerines in Tanzania"
    )

    val future = hybridStore.updateRecord(record)

    val updatedFuture = future.flatMap { _ =>
      hybridStore.updateRecord(updatedRecord)
    }

    whenReady(updatedFuture) { _ =>
      val dynamoRecord = Scanamo.get[HybridRecord](dynamoDbClient)(tableName)('id -> record.id).get
      dynamoRecord.isRight shouldBe true
      val hybridRecord = dynamoRecord.right.get

      hybridRecord.version shouldBe (updatedRecord.version + 1)
      hybridRecord.sourceId shouldBe updatedRecord.sourceId
      hybridRecord.sourceName shouldBe updatedRecord.sourceName

      val s3key = hybridRecord.s3key

      val retrievedJson = getJsonFromS3(bucketName = bucketName, key = s3key).noSpaces
      assertJsonStringsAreEqual(retrievedJson, toJson(updatedRecord.copy(version = updatedRecord.version + 1)).get)
    }
  }

  it("throws a ConditionalCheckFailedException if it gets an older version of an existing record") {
    val record = ExampleRecord(
      version = 4,
      sourceId = "4444",
      sourceName = "Test4444",
      content = "Four fiery foxes freezing in Finland"
    )

    val olderRecord = record.copy(
      version = 1,
      content = "Old otters eating oats"
    )

    val future = hybridStore.updateRecord(record)

    val updatedFuture = future.flatMap { _ =>
      hybridStore.updateRecord(olderRecord)
    }

    whenReady(updatedFuture.failed) { ex =>
      ex shouldBe a[ConditionalCheckFailedException]
    }
  }

  it("returns a future of None for a non-existent record") {
    val future = hybridStore.getRecord[ExampleRecord](id = "does/notexist")

    whenReady(future) { result =>
      result shouldBe None
    }
  }

  it("returns a future of Some[ExampleRecord] if the record exists") {
    val record = ExampleRecord(
      version = 5,
      sourceId = "5555",
      sourceName = "Test5555",
      content = "Five fishing flinging flint"
    )

    val putFuture = hybridStore.updateRecord(record)

    val getFuture = putFuture.flatMap { _ =>
      hybridStore.getRecord[ExampleRecord](record.id)
    }

    whenReady(getFuture) { result =>
      result shouldBe Some(record.copy(version = record.version + 1))
    }
  }
}
