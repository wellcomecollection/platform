package uk.ac.wellcome.storage.vhs

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.storage.s3.S3StringStore
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.storage.GlobalExecutionContext.context
import uk.ac.wellcome.storage.type_classes.StorageStrategy

import scala.util.Random


class StringStoreVersionedHybridStoreTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ExtendedPatience
    with LocalVersionedHybridStore {

  import uk.ac.wellcome.storage.dynamo._

  implicit val store: StorageStrategy[String] =
    StorageStrategy.stringStore

  def withS3StringStoreFixtures[R](
    testWith: TestWith[(Bucket,
                        Table,
                        VersionedHybridStore[String, S3StringStore]),
                       R]
  ): R =
    withLocalS3Bucket[R] { bucket =>
      withLocalDynamoDbTable[R] { table =>
        withStringVHS[R](bucket, table) { vhs =>
          testWith((bucket, table, vhs))
        }
      }
    }

  // The StringStore demonstrates the base functionality of the VHS

  describe("with S3StringStore") {
    it("stores a record if it has never been seen before") {
      withS3StringStoreFixtures {
        case (bucket, table, hybridStore) =>
          val id = Random.nextString(5)
          val record = "One ocelot in orange"

          val future = hybridStore.updateRecord(id)(record)(identity)()

          whenReady(future) { _ =>
            getContentFor(bucket, table, id) shouldBe record
          }
      }
    }

    it("applies the given transformation to an existing record") {
      withS3StringStoreFixtures {
        case (bucket, table, hybridStore) =>
          val id = Random.nextString(5)
          val record = "Two teal turtles in Tenerife"

          val updatedRecord =
            "Throwing turquoise tangerines in Tanzania"

          val future =
            hybridStore.updateRecord(id)(record)(identity)()

          val updatedFuture = future.flatMap { _ =>
            hybridStore.updateRecord(id)(updatedRecord)(_ => updatedRecord)()
          }

          whenReady(updatedFuture) { _ =>
            getContentFor(bucket, table, id) shouldBe updatedRecord
          }
      }
    }

    it("returns a future of None for a non-existent record") {
      withS3StringStoreFixtures {
        case (_, _, hybridStore) =>
          val future = hybridStore.getRecord(id = "does/notexist")

          whenReady(future) { result =>
            result shouldBe None
          }
      }
    }

    it("returns a future of Some[String] if the record exists") {
      withS3StringStoreFixtures {
        case (_, _, hybridStore) =>
          val id = Random.nextString(5)
          val record = "Five fishing flinging flint"

          val putFuture =
            hybridStore.updateRecord(id)(record)(identity)()

          val getFuture = putFuture.flatMap { _ =>
            hybridStore.getRecord(id)
          }

          whenReady(getFuture) { result =>
            result shouldBe Some(record)
          }
      }
    }

    it("can store additional metadata alongside HybridRecord") {
      case class ExtraData(
        data: String,
        number: Int
      )

      val id = Random.nextString(5)
      val record = "this goes in dynamo"

      val data = ExtraData(
        data = "a tragic triangle of triffids",
        number = 6
      )

      withS3StringStoreFixtures {
        case (_, table, hybridStore) =>
          val future =
            hybridStore.updateRecord(id)(record)(identity)(data)

          whenReady(future) { _ =>
            val maybeResult =
              Scanamo.get[ExtraData](dynamoDbClient)(table.name)('id -> id)

            maybeResult shouldBe defined
            maybeResult.get.isRight shouldBe true

            val extraData = maybeResult.get.right.get

            extraData.data shouldBe "a tragic triangle of triffids"
            extraData.number shouldBe 6
          }
      }
    }
  }
}
