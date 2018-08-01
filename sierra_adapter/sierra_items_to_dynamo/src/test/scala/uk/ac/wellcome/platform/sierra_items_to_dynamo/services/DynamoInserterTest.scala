package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures.DynamoInserterFixture
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VHSConfig, VersionedHybridStore}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class DynamoInserterTest
    extends FunSpec
    with Matchers
    with DynamoInserterFixture
    with ScalaFutures
    with MockitoSugar
    with ExtendedPatience
    with SierraUtil {

  it("inserts an ItemRecord into the VHS") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withDynamoInserter(table, bucket) { dynamoInserter =>
          val record = createSierraItemRecord

          val futureUnit = dynamoInserter.insertIntoDynamo(record)

          whenReady(futureUnit) { _ =>
            assertStored[SierraItemRecord](
              bucket = bucket,
              table = table,
              id = record.id.withoutCheckDigit,
              record = record
            )
          }
        }
      }
    }
  }

  it("does not overwrite new data with old data") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withTypeVHS[SierraItemRecord,
                    EmptyMetadata,
                    Assertion](bucket, table) { versionedHybridStore =>
          val dynamoInserter = new DynamoInserter(versionedHybridStore)

          val newRecord = createSierraItemRecordWith(
            modifiedDate = newerDate,
            bibIds = List(createSierraBibNumber)
          )
          insertRecord(newRecord, versionedHybridStore = versionedHybridStore)

          val oldRecord = createSierraItemRecordWith(
            id = newRecord.id,
            modifiedDate = olderDate,
            bibIds = List(createSierraBibNumber)
          )

          val futureUnit = dynamoInserter.insertIntoDynamo(oldRecord)
          whenReady(futureUnit) { _ =>
            assertStored[SierraItemRecord](
              bucket = bucket,
              table = table,
              id = oldRecord.id.withoutCheckDigit,
              record = newRecord
            )
          }
        }
      }
    }
  }

  it("overwrites old data with new data") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withTypeVHS[SierraItemRecord,
                    EmptyMetadata,
                    Assertion](bucket, table) { versionedHybridStore =>
          val dynamoInserter = new DynamoInserter(versionedHybridStore)

          val oldRecord = createSierraItemRecordWith(
            modifiedDate = olderDate,
            bibIds = List(createSierraBibNumber)
          )
          insertRecord(oldRecord, versionedHybridStore = versionedHybridStore)

          val newRecord = createSierraItemRecordWith(
            id = oldRecord.id,
            modifiedDate = newerDate,
            bibIds = oldRecord.bibIds ++ List(createSierraBibNumber)
          )

          val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

          whenReady(futureUnit) { _ =>
            assertStored[SierraItemRecord](
              bucket = bucket,
              table = table,
              id = oldRecord.id.withoutCheckDigit,
              record = newRecord
            )
          }
        }
      }
    }
  }

  it("records unlinked bibIds") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withTypeVHS[SierraItemRecord,
                    EmptyMetadata,
                    Assertion](bucket, table) { versionedHybridStore =>
          val dynamoInserter = new DynamoInserter(versionedHybridStore)
          val bibIds = createSierraBibNumbers(count = 3)

          val oldRecord = createSierraItemRecordWith(
            modifiedDate = olderDate,
            bibIds = bibIds
          )
          insertRecord(oldRecord, versionedHybridStore = versionedHybridStore)

          val newRecord = createSierraItemRecordWith(
            id = oldRecord.id,
            modifiedDate = newerDate,
            bibIds = List(bibIds(0), bibIds(1))
          )

          val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

          whenReady(futureUnit) { _ =>
            assertStored[SierraItemRecord](
              bucket = bucket,
              table = table,
              id = oldRecord.id.withoutCheckDigit,
              record = newRecord.copy(unlinkedBibIds = List(bibIds(2)))
            )
          }
        }
      }
    }
  }

  it("adds new bibIds and records unlinked bibIds in the same update") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withTypeVHS[SierraItemRecord,
                    EmptyMetadata,
                    Assertion](bucket, table) { versionedHybridStore =>
          val dynamoInserter = new DynamoInserter(versionedHybridStore)
          val bibIds = createSierraBibNumbers(count = 4)

          val oldRecord = createSierraItemRecordWith(
            modifiedDate = olderDate,
            bibIds = List(bibIds(0), bibIds(1), bibIds(2))
          )
          insertRecord(oldRecord, versionedHybridStore = versionedHybridStore)

          val newRecord = createSierraItemRecordWith(
            id = oldRecord.id,
            modifiedDate = newerDate,
            bibIds = List(bibIds(1), bibIds(2), bibIds(3))
          )

          val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

          whenReady(futureUnit) { _ =>
            assertStored[SierraItemRecord](
              bucket = bucket,
              table = table,
              id = oldRecord.id.withoutCheckDigit,
              record = newRecord.copy(unlinkedBibIds = List(bibIds(0)))
            )
          }
        }
      }
    }
  }

  it("preserves existing unlinked bibIds in DynamoDB") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withTypeVHS[SierraItemRecord,
                    EmptyMetadata,
                    Assertion](bucket, table) { versionedHybridStore =>
          val dynamoInserter = new DynamoInserter(versionedHybridStore)
          val bibIds = createSierraBibNumbers(count = 5)

          val oldRecord = createSierraItemRecordWith(
            modifiedDate = olderDate,
            bibIds = List(bibIds(0), bibIds(1), bibIds(2)),
            unlinkedBibIds = List(bibIds(4))
          )
          insertRecord(oldRecord, versionedHybridStore = versionedHybridStore)

          val newRecord = createSierraItemRecordWith(
            id = oldRecord.id,
            modifiedDate = newerDate,
            bibIds = List(bibIds(1), bibIds(2), bibIds(3)),
            unlinkedBibIds = List()
          )

          val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

          whenReady(futureUnit) { _ =>
            val actualContents = getContentFor(
              bucket = bucket,
              table = table,
              id = oldRecord.id.withoutCheckDigit
            )

            val actualRecord = fromJson[SierraItemRecord](actualContents).get
            actualRecord.unlinkedBibIds shouldBe List(bibIds(4), bibIds(0))
          }

        }
      }
    }
  }

  it("fails if the VHS returns an error when updating an item") {
    val record = createSierraItemRecordWith(
      modifiedDate = newerDate
    )

    val mockedVhs = new VersionedHybridStore[
      SierraItemRecord,
      EmptyMetadata,
      ObjectStore[SierraItemRecord]](
      vhsConfig = VHSConfig(
        dynamoConfig = DynamoConfig(table = "doesnotexist", maybeIndex = None),
        s3Config = S3Config(bucketName = "nosuchbucket"),
        globalS3Prefix = ""
      ),
      objectStore = ObjectStore[SierraItemRecord],
      dynamoDbClient = dynamoDbClient
    )

    val dynamoInserter = new DynamoInserter(mockedVhs)

    val futureUnit = dynamoInserter.insertIntoDynamo(record)
    whenReady(futureUnit.failed) { ex =>
      ex shouldBe a[ResourceNotFoundException]
      ex.getMessage should startWith("Cannot do operations on a non-existent table")
    }
  }

  def storeSingleRecord[T, Metadata](
                                      versionedHybridStore: VersionedHybridStore[T, Metadata, ObjectStore[T]],
                                      id: String,
                                      record: T,
                                      metadata: Metadata
                                    ): Assertion = {
    val putFuture = versionedHybridStore.updateRecord(id = id)(
      ifNotExisting = (record, metadata)
    )(
      ifExisting = (existingRecord, existingMetadata) =>
        throw new RuntimeException(s"VHS should be empty; got ($existingRecord, $existingMetadata)!")
    )

    whenReady(putFuture) { _ =>
      val getFuture = versionedHybridStore.getRecord(id = id)
      whenReady(getFuture) { result =>
        result.get shouldBe record
      }
    }
  }

  def storeSingleRecord[T](
                            versionedHybridStore: VersionedHybridStore[T, EmptyMetadata, ObjectStore[T]],
                            id: String,
                            record: T,
                          ): Assertion = storeSingleRecord[T, EmptyMetadata](
    versionedHybridStore = versionedHybridStore,
    id = id,
    record = record,
    metadata = EmptyMetadata()
  )

  private def insertRecord(
    itemRecord: SierraItemRecord,
    versionedHybridStore: VersionedHybridStore[SierraItemRecord,
                                               EmptyMetadata,
                                               ObjectStore[SierraItemRecord]]) = {
    storeSingleRecord(
      versionedHybridStore = versionedHybridStore,
      id = itemRecord.id.withoutCheckDigit,
      record = itemRecord
    )
  }
}
