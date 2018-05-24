package uk.ac.wellcome.storage.vhs

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.storage.s3.S3StringStore
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.storage.GlobalExecutionContext.context

import scala.util.Random

class StringStoreVersionedHybridStoreTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ExtendedPatience
    with LocalVersionedHybridStore {

  import uk.ac.wellcome.storage.dynamo._

  def withS3StringStoreFixtures[R](
    testWith: TestWith[
      (Bucket,
       Table,
       VersionedHybridStore[String, EmptyMetadata, S3StringStore]),
      R]
  ): R =
    withLocalS3Bucket[R] { bucket =>
      withLocalDynamoDbTable[R] { table =>
        withStringVHS[EmptyMetadata, R](bucket, table) { vhs =>
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

          val future =
            hybridStore.updateRecord(id)(record)((t, _) => t)(EmptyMetadata())

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
            hybridStore.updateRecord(id)(record)((t, _) => t)(EmptyMetadata())
          val updatedFuture = future.flatMap { _ =>
            hybridStore.updateRecord(id)(updatedRecord)(
              (t, _) => updatedRecord)(EmptyMetadata())
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
            hybridStore.updateRecord(id)(record)((t, _) => t)(EmptyMetadata())

          val getFuture = putFuture.flatMap { _ =>
            hybridStore.getRecord(id)
          }

          whenReady(getFuture) { result =>
            result shouldBe Some(record)
          }
      }
    }

    describe("with metadata") {

      case class ExtraData(
        data: String,
        number: Int
      )

      val id = Random.nextString(5)
      val record = Random.nextString(256)

      val data = ExtraData(
        data = Random.nextString(256),
        number = Random.nextInt(256)
      )

      it("can store additional metadata") {
        withLocalS3Bucket { bucket =>
          withLocalDynamoDbTable { table =>
            withStringVHS[ExtraData, Assertion](bucket, table) { hybridStore =>
              val future =
                hybridStore.updateRecord(id)(record)((t, _) => t)(data)

              whenReady(future) { _ =>
                val maybeResult =
                  Scanamo.get[ExtraData](dynamoDbClient)(table.name)('id -> id)

                maybeResult shouldBe defined
                maybeResult.get.isRight shouldBe true

                val extraData = maybeResult.get.right.get

                extraData.data shouldBe data.data
                extraData.number shouldBe data.number
              }
            }
          }
        }
      }

      it("provides stored metadata when updating a record") {
        withLocalS3Bucket { bucket =>
          withLocalDynamoDbTable { table =>
            withStringVHS[ExtraData, Assertion](bucket, table) { hybridStore =>
              val updatedRecord = Random.nextString(256)

              val future =
                hybridStore.updateRecord(id)(record)((t, _) => t)(data)

              val updatedFuture = future.flatMap { _ =>
                hybridStore.updateRecord(id)(updatedRecord)((t, m) =>
                  m.toString)(data)
              }

              whenReady(updatedFuture) { _ =>
                getContentFor(bucket, table, id) shouldBe data.toString
              }
            }
          }
        }
      }

    }
  }
}
