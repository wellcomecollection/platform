package uk.ac.wellcome.storage.vhs

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.storage.GlobalExecutionContext.context
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3Config

import scala.util.Random

class StringStoreVersionedHybridStoreTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ExtendedPatience
    with LocalVersionedHybridStore {

  import uk.ac.wellcome.storage.dynamo._

  def withStringVHS[Metadata, R](
    bucket: Bucket,
    table: Table,
    globalS3Prefix: String = defaultGlobalS3Prefix)(
    testWith: TestWith[
      VersionedHybridStore[String, Metadata, ObjectStore[String]],
      R])(
    implicit objectStore: ObjectStore[String]
  ): R = {
    val s3Config = S3Config(bucketName = bucket.name)

    val dynamoConfig =
      DynamoConfig(table = table.name, maybeIndex = Some(table.index))

    val vhsConfig = VHSConfig(
      dynamoConfig = dynamoConfig,
      s3Config = s3Config,
      globalS3Prefix = globalS3Prefix
    )

    val store =
      new VersionedHybridStore[String, Metadata, ObjectStore[String]](
        vhsConfig = vhsConfig,
        objectStore = objectStore,
        dynamoDbClient = dynamoDbClient
      )

    testWith(store)
  }

  def withS3StringStoreFixtures[R](
    testWith: TestWith[
      (Bucket,
       Table,
       VersionedHybridStore[String, EmptyMetadata, ObjectStore[String]]),
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

          val future = hybridStore.updateRecord(id)(ifNotExisting =
            (record, EmptyMetadata()))(ifExisting = (t, m) => (t, m))

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
          val updatedRecord = "Throwing turquoise tangerines in Tanzania"

          val future =
            hybridStore.updateRecord(id)((record, EmptyMetadata()))((t, m) =>
              (t, m))
          val updatedFuture = future.flatMap { _ =>
            hybridStore.updateRecord(id)(ifNotExisting =
              (updatedRecord, EmptyMetadata()))(ifExisting = (_, m) =>
              (updatedRecord, EmptyMetadata()))
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
            hybridStore.updateRecord(id)(ifNotExisting =
              (record, EmptyMetadata()))(ifExisting = (t, m) => (t, m))

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

      val storedMetadata = ExtraData(
        data = Random.nextString(256),
        number = Random.nextInt(256)
      )

      it("can store additional metadata") {
        withLocalS3Bucket { bucket =>
          withLocalDynamoDbTable { table =>
            withStringVHS[ExtraData, Assertion](bucket, table) { hybridStore =>
              val future =
                hybridStore.updateRecord(id)(ifNotExisting =
                  (record, storedMetadata))(ifExisting = (t, m) => (t, m))

              whenReady(future) { _ =>
                getRecordMetadata[ExtraData](table, id) shouldBe storedMetadata
              }
            }
          }
        }
      }

      it("provides stored metadata when updating a record") {
        withLocalS3Bucket { bucket =>
          withLocalDynamoDbTable { table =>
            withStringVHS[ExtraData, Assertion](bucket, table) { hybridStore =>
              val updatedMetadata = ExtraData("new-metadata", 22)
              val future = hybridStore.updateRecord(id)(ifNotExisting =
                (record, storedMetadata))(ifExisting = (t, m) => (t, m))

              val updatedFuture = future.flatMap { _ =>
                hybridStore.updateRecord(id)(ifNotExisting =
                  ("not-this", ExtraData("x", 1)))(ifExisting = (t, _) =>
                  (t, updatedMetadata))
              }

              whenReady(updatedFuture) { _ =>
                getRecordMetadata[ExtraData](table, id) shouldBe updatedMetadata
              }
            }
          }
        }
      }
    }
  }
}
