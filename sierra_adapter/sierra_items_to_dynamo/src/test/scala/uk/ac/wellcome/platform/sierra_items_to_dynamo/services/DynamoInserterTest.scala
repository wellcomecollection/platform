package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators
import uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures.DynamoInserterFixture
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.EmptyMetadata

class DynamoInserterTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MockitoSugar
    with DynamoInserterFixture
    with IntegrationPatience
    with SierraGenerators {

  it("inserts an ItemRecord into the VHS") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withDynamoInserter(table, bucket) { dynamoInserter =>
          val record = createSierraItemRecord

          val futureUnit = dynamoInserter.insertIntoDynamo(record)

          whenReady(futureUnit) { _ =>
            assertStored[SierraItemRecord](
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
        withItemRecordVHS(table, bucket) { versionedHybridStore =>
          val dynamoInserter = new DynamoInserter(versionedHybridStore)

          val newRecord = createSierraItemRecordWith(
            modifiedDate = newerDate,
            bibIds = List(createSierraBibNumber)
          )
          storeSingleRecord(
            newRecord,
            versionedHybridStore = versionedHybridStore)

          val oldRecord = createSierraItemRecordWith(
            id = newRecord.id,
            modifiedDate = olderDate,
            bibIds = List(createSierraBibNumber)
          )

          val futureUnit = dynamoInserter.insertIntoDynamo(oldRecord)
          whenReady(futureUnit) { _ =>
            assertStored[SierraItemRecord](
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
        withItemRecordVHS(table, bucket) { versionedHybridStore =>
          val dynamoInserter = new DynamoInserter(versionedHybridStore)

          val oldRecord = createSierraItemRecordWith(
            modifiedDate = olderDate,
            bibIds = List(createSierraBibNumber)
          )
          storeSingleRecord(
            oldRecord,
            versionedHybridStore = versionedHybridStore)

          val newRecord = createSierraItemRecordWith(
            id = oldRecord.id,
            modifiedDate = newerDate,
            bibIds = oldRecord.bibIds ++ List(createSierraBibNumber)
          )

          val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

          whenReady(futureUnit) { _ =>
            assertStored[SierraItemRecord](
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
        withItemRecordVHS(table, bucket) { versionedHybridStore =>
          val dynamoInserter = new DynamoInserter(versionedHybridStore)
          val bibIds = createSierraBibNumbers(count = 3)

          val oldRecord = createSierraItemRecordWith(
            modifiedDate = olderDate,
            bibIds = bibIds
          )
          storeSingleRecord(
            oldRecord,
            versionedHybridStore = versionedHybridStore)

          val newRecord = createSierraItemRecordWith(
            id = oldRecord.id,
            modifiedDate = newerDate,
            bibIds = List(bibIds(0), bibIds(1))
          )

          val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

          whenReady(futureUnit) { _ =>
            assertStored[SierraItemRecord](
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
        withItemRecordVHS(table, bucket) { versionedHybridStore =>
          val dynamoInserter = new DynamoInserter(versionedHybridStore)
          val bibIds = createSierraBibNumbers(count = 4)

          val oldRecord = createSierraItemRecordWith(
            modifiedDate = olderDate,
            bibIds = List(bibIds(0), bibIds(1), bibIds(2))
          )
          storeSingleRecord(
            oldRecord,
            versionedHybridStore = versionedHybridStore)

          val newRecord = createSierraItemRecordWith(
            id = oldRecord.id,
            modifiedDate = newerDate,
            bibIds = List(bibIds(1), bibIds(2), bibIds(3))
          )

          val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

          whenReady(futureUnit) { _ =>
            assertStored[SierraItemRecord](
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
        withItemRecordVHS(table, bucket) { versionedHybridStore =>
          val dynamoInserter = new DynamoInserter(versionedHybridStore)
          val bibIds = createSierraBibNumbers(count = 5)

          val oldRecord = createSierraItemRecordWith(
            modifiedDate = olderDate,
            bibIds = List(bibIds(0), bibIds(1), bibIds(2)),
            unlinkedBibIds = List(bibIds(4))
          )
          storeSingleRecord(
            oldRecord,
            versionedHybridStore = versionedHybridStore)

          val newRecord = createSierraItemRecordWith(
            id = oldRecord.id,
            modifiedDate = newerDate,
            bibIds = List(bibIds(1), bibIds(2), bibIds(3)),
            unlinkedBibIds = List()
          )

          val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

          whenReady(futureUnit) { _ =>
            val actualContents = getContentFor(
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

    val badTable = Table("doesnotexist", index = "nosuchindex")
    val badBucket = Bucket("nosuchbucket")

    withDynamoInserter(badTable, badBucket) { dynamoInserter =>
      val futureUnit = dynamoInserter.insertIntoDynamo(record)
      whenReady(futureUnit.failed) { ex =>
        ex shouldBe a[ResourceNotFoundException]
        ex.getMessage should startWith(
          "Cannot do operations on a non-existent table")
      }
    }
  }

  def storeSingleRecord(
    itemRecord: SierraItemRecord,
    versionedHybridStore: SierraItemsVHS
  ): Assertion = {
    val putFuture =
      versionedHybridStore.updateRecord(id = itemRecord.id.withoutCheckDigit)(
        ifNotExisting = (itemRecord, EmptyMetadata())
      )(
        ifExisting = (existingRecord, existingMetadata) =>
          throw new RuntimeException(
            s"VHS should be empty; got ($existingRecord, $existingMetadata)!")
      )

    whenReady(putFuture) { _ =>
      val getFuture =
        versionedHybridStore.getRecord(id = itemRecord.id.withoutCheckDigit)
      whenReady(getFuture) { result =>
        result.get shouldBe itemRecord
      }
    }
  }
}
