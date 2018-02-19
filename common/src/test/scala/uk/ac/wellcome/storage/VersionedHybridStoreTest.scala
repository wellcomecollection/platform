package uk.ac.wellcome.storage

import com.gu.scanamo.DynamoFormat
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{Sourced, VersionUpdater, Versioned}
import uk.ac.wellcome.utils.GlobalExecutionContext._
import uk.ac.wellcome.utils.JsonUtil._

case class ExampleRecord(
  version: Int,
  sourceId: String,
  sourceName: String,
  content: String
) extends Versioned
    with Sourced

class VersionedHybridStoreTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with VersionedHybridStoreLocal {

  implicit val testVersionUpdater = new VersionUpdater[ExampleRecord] {
    override def updateVersion(testVersioned: ExampleRecord,
                               newVersion: Int): ExampleRecord = {
      testVersioned.copy(version = newVersion)
    }
  }

  override lazy val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  override lazy val tableName: String = "versioned-hybrid-store-test"
  override lazy val bucketName = "versioned-hybrid-store-test"

  it("stores a versioned record if it has never been seen before") {
    val record = ExampleRecord(
      version = 1,
      sourceId = "1111",
      sourceName = "Test1111",
      content = "One ocelot in orange"
    )

    val expectedRecord = record.copy(version = 2)

    val future = hybridStore.updateRecord(record.sourceName, record.sourceId)(
      record)(identity)

    whenReady(future) { _ =>
      assertHybridRecordIsStoredCorrectly(
        record = expectedRecord,
        expectedJson = toJson(expectedRecord).get
      )
    }
  }

  it("applies the given transformation to an existing record") {
    val record = ExampleRecord(
      version = 1,
      sourceId = "1111",
      sourceName = "Test1111",
      content = "One ocelot in orange"
    )

    val expectedRecord = record
      .copy(
        content = "new content",
        version = 3
      )

    val t = (e: ExampleRecord) => e.copy(content = "new content")

    val future =
      hybridStore
        .updateRecord(record.sourceName, record.sourceId)(record)(identity)
        .flatMap(_ =>
          hybridStore.updateRecord(record.sourceName, record.sourceId)(record)(
            t))

    whenReady(future) { _ =>
      assertHybridRecordIsStoredCorrectly(
        record = expectedRecord,
        expectedJson = toJson(expectedRecord).get
      )
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

    val expectedRecord = updatedRecord.copy(version = 4)

    val future = hybridStore.updateRecord(record.sourceName, record.sourceId)(
      record)(identity)

    val updatedFuture = future.flatMap { _ =>
      hybridStore.updateRecord(updatedRecord.sourceName,
                               updatedRecord.sourceId)(updatedRecord)(_ =>
        updatedRecord)
    }

    whenReady(updatedFuture) { _ =>
      assertHybridRecordIsStoredCorrectly(
        record = expectedRecord,
        expectedJson = toJson(expectedRecord).get
      )
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

    val putFuture = hybridStore.updateRecord(record.sourceName,
                                             record.sourceId)(record)(identity)

    val getFuture = putFuture.flatMap { _ =>
      hybridStore.getRecord[ExampleRecord](record.id)
    }

    whenReady(getFuture) { result =>
      result shouldBe Some(record.copy(version = record.version + 1))
    }
  }

  it("does not create a new version of a record if it's not modified") {
    val record = ExampleRecord(
      version = 0,
      sourceId = "5555",
      sourceName = "Test5555",
      content = "Five fishing flinging flint"
    )

    val future = for {
      _ <- hybridStore.updateRecord(record.sourceName, record.sourceId)(
        record)(identity)
      _ <- hybridStore.updateRecord(record.sourceName, record.sourceId)(
        record)(identity)
      result <- hybridStore.getRecord[ExampleRecord](record.id)
    } yield result

    whenReady(future) { result =>
      result.get.version shouldBe 1
    }
  }

  it("does not allow creation of records with a different id than indicated") {
    val record = ExampleRecord(
      version = 0,
      sourceId = "8934",
      sourceName = "Test5555",
      content = "Five fishing flinging flint"
    )

    val future =
      hybridStore.updateRecord("sierra", "not_the_same_id")(record)(identity)

    whenReady(future.failed) { e: Throwable =>
      e shouldBe a[IllegalArgumentException]
    }
  }

  it("does not allow transformation to a record with a different id") {
    val record = ExampleRecord(
      version = 0,
      sourceId = "8934",
      sourceName = "Test5555",
      content = "Five fishing flinging flint"
    )

    val recordWithDifferentId = record.copy(sourceId = "not_the_same_id")

    val future = for {
      _ <- hybridStore.updateRecord(record.sourceName, record.sourceId)(
        record)(identity)
      _ <- hybridStore.updateRecord(record.sourceName, record.sourceId)(
        record)(_ => recordWithDifferentId)
    } yield ()

    whenReady(future.failed) { e: Throwable =>
      e shouldBe a[IllegalArgumentException]
    }
  }
}
