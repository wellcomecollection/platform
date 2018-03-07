package uk.ac.wellcome.storage

import com.gu.scanamo.{DynamoFormat, Scanamo}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{Id, Sourced, Versioned}
import uk.ac.wellcome.s3.{KeyPrefixGenerator, S3ObjectStore}
import uk.ac.wellcome.utils.GlobalExecutionContext._
import uk.ac.wellcome.utils.JsonUtil._
import com.gu.scanamo.syntax._

import scala.annotation.Annotation

case class ExampleRecord(
  id: String,
  content: String
) extends Id

class VersionedHybridStoreTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with VersionedHybridStoreLocal {

  import uk.ac.wellcome.dynamo._

  val hybridStore = createHybridStore[ExampleRecord]

  override lazy val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  override lazy val tableName: String = "versioned-hybrid-store-test"
  override lazy val bucketName = "versioned-hybrid-store-test"

  it("stores a versioned record if it has never been seen before") {
    val record = ExampleRecord(
      id = "1111",
      content = "One ocelot in orange"
    )

    val future = hybridStore.updateRecord(record.id)(record)(identity)()

    whenReady(future) { _ =>
      assertHybridRecordIsStoredCorrectly(
        record = record,
        expectedJson = toJson(record).get
      )
    }
  }

  it("applies the given transformation to an existing record") {
    val record = ExampleRecord(
      id = "1111",
      content = "One ocelot in orange"
    )

    val expectedRecord = record
      .copy(content = "new content")

    val t = (e: ExampleRecord) => e.copy(content = "new content")

    val future =
      hybridStore
        .updateRecord(record.id)(record)(identity)()
        .flatMap(_ => hybridStore.updateRecord(record.id)(record)(t)())

    whenReady(future) { _ =>
      assertHybridRecordIsStoredCorrectly(
        record = expectedRecord,
        expectedJson = toJson(expectedRecord).get
      )
    }

  }

  it("updates DynamoDB and S3 if it sees a new version of a record") {
    val record = ExampleRecord(
      id = "2222",
      content = "Two teal turtles in Tenerife"
    )

    val updatedRecord = record.copy(
      content = "Throwing turquoise tangerines in Tanzania"
    )

    val future = hybridStore.updateRecord(record.id)(record)(identity)()

    val updatedFuture = future.flatMap { _ =>
      hybridStore.updateRecord(updatedRecord.id)(updatedRecord)(_ =>
        updatedRecord)()
    }

    whenReady(updatedFuture) { _ =>
      assertHybridRecordIsStoredCorrectly(
        record = updatedRecord,
        expectedJson = toJson(updatedRecord).get
      )
    }
  }

  it("returns a future of None for a non-existent record") {
    val future = hybridStore.getRecord(id = "does/notexist")

    whenReady(future) { result =>
      result shouldBe None
    }
  }

  it("returns a future of Some[ExampleRecord] if the record exists") {
    val record = ExampleRecord(
      id = "5555",
      content = "Five fishing flinging flint"
    )

    val putFuture = hybridStore.updateRecord(record.id)(record)(identity)()

    val getFuture = putFuture.flatMap { _ =>
      hybridStore.getRecord(record.id)
    }

    whenReady(getFuture) { result =>
      result shouldBe Some(record)
    }
  }

  it("does not allow creation of records with a different id than indicated") {
    val record = ExampleRecord(
      id = "8934",
      content = "Five fishing flinging flint"
    )

    val future =
      hybridStore.updateRecord(id = "not_the_same_id")(record)(identity)()

    whenReady(future.failed) { e: Throwable =>
      e shouldBe a[IllegalArgumentException]
    }
  }

  it("does not allow transformation to a record with a different id") {
    val record = ExampleRecord(
      id = "8934",
      content = "Five fishing flinging flint"
    )

    val recordWithDifferentId = record.copy(id = "not_the_same_id")

    val future = for {
      _ <- hybridStore.updateRecord(record.id)(record)(identity)()
      _ <- hybridStore.updateRecord(record.id)(record)(_ =>
        recordWithDifferentId)()
    } yield ()

    whenReady(future.failed) { e: Throwable =>
      e shouldBe a[IllegalArgumentException]
    }
  }

  it("can store additional metadata alongside HybridRecord") {
    case class ExampleRecord(
      id: String,
      content: String
    ) extends Id

    case class ExtraData(
      data: String,
      number: Int
    )

    val content = "this goes in dynamo"

    val record = ExampleRecord(
      id = "11111",
      content = content
    )

    val data = ExtraData(
      data = "a tragic triangle of triffids",
      number = 6
    )

    val hybridStore = createHybridStore[ExampleRecord]

    val future = hybridStore.updateRecord(record.id)(record)(identity)(data)

    whenReady(future) { _ =>
      val maybeResult =
        Scanamo.get[ExtraData](dynamoDbClient)(tableName)('id -> record.id)

      maybeResult shouldBe defined
      maybeResult.get.isRight shouldBe true

      val extraData = maybeResult.get.right.get

      extraData.data shouldBe "a tragic triangle of triffids"
      extraData.number shouldBe 6
    }

  }
}
