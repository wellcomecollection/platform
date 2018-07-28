package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.syntax._
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.type_classes.{
  IdGetter,
  VersionGetter,
  VersionUpdater
}
import uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures.DynamoInserterFixture

import scala.concurrent.Future
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.ExecutionContext.Implicits.global

class DynamoInserterTest
    extends FunSpec
    with Matchers
    with DynamoInserterFixture
    with ScalaFutures
    with MockitoSugar
    with ExtendedPatience
    with SierraUtil {

  it("ingests a json item into DynamoDB") {
    withLocalDynamoDbTable { table =>
      withDynamoInserter(table) { dynamoInserter =>
        val record = createSierraItemRecord

        val futureUnit = dynamoInserter.insertIntoDynamo(record)

        whenReady(futureUnit) { _ =>
          Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)(
            'id -> record.id) shouldBe Some(Right(record.copy(version = 1)))
        }
      }
    }
  }

  it("does not overwrite new data with old data") {
    withLocalDynamoDbTable { table =>
      withDynamoInserter(table) { dynamoInserter =>
        val newRecord = createSierraItemRecordWith(
          modifiedDate = newerDate,
          bibIds = List(createSierraBibNumber)
        )
        Scanamo.put(dynamoDbClient)(table.name)(newRecord)

        val oldRecord = newRecord.copy(
          modifiedDate = olderDate,
          bibIds = List(createSierraBibNumber)
        )

        val futureUnit = dynamoInserter.insertIntoDynamo(oldRecord)
        whenReady(futureUnit) { _ =>
          Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)(
            'id -> newRecord.id) shouldBe Some(Right(newRecord))
        }
      }
    }
  }

  it("overwrites old data with new data") {
    withLocalDynamoDbTable { table =>
      withDynamoInserter(table) { dynamoInserter =>
        val oldRecord = createSierraItemRecordWith(
          modifiedDate = olderDate,
          bibIds = List(createSierraBibNumber)
        )
        Scanamo.put(dynamoDbClient)(table.name)(oldRecord)

        val newRecord = createSierraItemRecordWith(
          id = oldRecord.id,
          modifiedDate = newerDate,
          bibIds = oldRecord.bibIds ++ List(createSierraBibNumber)
        )

        val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

        whenReady(futureUnit) { _ =>
          Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)(
            'id -> oldRecord.id) shouldBe Some(
            Right(newRecord.copy(version = newRecord.version + 1)))
        }
      }
    }
  }

  it("records unlinked bibIds") {
    withLocalDynamoDbTable { table =>
      withDynamoInserter(table) { dynamoInserter =>
        val bibIds = createSierraBibNumbers(count = 3)

        val oldRecord = createSierraItemRecordWith(
          modifiedDate = olderDate,
          bibIds = bibIds
        )
        Scanamo.put(dynamoDbClient)(table.name)(oldRecord)

        val newRecord = createSierraItemRecordWith(
          id = oldRecord.id,
          modifiedDate = newerDate,
          bibIds = List(bibIds(0), bibIds(1))
        )

        val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

        whenReady(futureUnit) { _ =>
          Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)(
            'id -> oldRecord.id) shouldBe Some(
            Right(
              newRecord.copy(version = 1, unlinkedBibIds = List(bibIds(2)))))
        }
      }
    }
  }

  it("adds new bibIds and records unlinked bibIds in the same update") {
    withLocalDynamoDbTable { table =>
      withDynamoInserter(table) { dynamoInserter =>
        val bibIds = createSierraBibNumbers(count = 4)

        val oldRecord = createSierraItemRecordWith(
          modifiedDate = olderDate,
          bibIds = List(bibIds(0), bibIds(1), bibIds(2))
        )
        Scanamo.put(dynamoDbClient)(table.name)(oldRecord)

        val newRecord = createSierraItemRecordWith(
          id = oldRecord.id,
          modifiedDate = newerDate,
          bibIds = List(bibIds(1), bibIds(2), bibIds(3))
        )

        val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

        whenReady(futureUnit) { _ =>
          Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)(
            'id -> oldRecord.id) shouldBe Some(
            Right(
              newRecord.copy(version = 1, unlinkedBibIds = List(bibIds(0)))))
        }
      }
    }
  }

  it("preserves existing unlinked bibIds in DynamoDB") {
    withLocalDynamoDbTable { table =>
      withDynamoInserter(table) { dynamoInserter =>
        val bibIds = createSierraBibNumbers(count = 5)

        val oldRecord = createSierraItemRecordWith(
          modifiedDate = olderDate,
          bibIds = List(bibIds(0), bibIds(1), bibIds(2)),
          unlinkedBibIds = List(bibIds(4))
        )
        Scanamo.put(dynamoDbClient)(table.name)(oldRecord)

        val newRecord = createSierraItemRecordWith(
          id = oldRecord.id,
          modifiedDate = newerDate,
          bibIds = List(bibIds(1), bibIds(2), bibIds(3)),
          unlinkedBibIds = List()
        )

        val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

        whenReady(futureUnit) { _ =>
          val actualRecord: SierraItemRecord = Scanamo
            .get[SierraItemRecord](dynamoDbClient)(table.name)(
              'id -> oldRecord.id)
            .get
            .right
            .get

          actualRecord.unlinkedBibIds shouldBe List(bibIds(4), bibIds(0))
        }
      }
    }
  }

  it("fails if a dao returns an error when updating an item") {
    val record = createSierraItemRecordWith(
      modifiedDate = newerDate
    )

    val mockedDao = mock[VersionedDao]
    val expectedException = new RuntimeException("AAAAAARGH!")

    when(
      mockedDao.getRecord[SierraItemRecord](any[String])(
        any[DynamoFormat[SierraItemRecord]]))
      .thenReturn(
        Future.successful(
          Some(
            createSierraItemRecordWith(
              id = record.id,
              modifiedDate = olderDate
            )
          )))

    when(
      mockedDao.updateRecord(any[SierraItemRecord])(
        any[DynamoFormat[SierraItemRecord]],
        any[VersionUpdater[SierraItemRecord]],
        any[IdGetter[SierraItemRecord]],
        any[VersionGetter[SierraItemRecord]],
        any[UpdateExpressionGenerator[SierraItemRecord]]
      ))
      .thenThrow(expectedException)

    val dynamoInserter = new DynamoInserter(mockedDao)

    val futureUnit = dynamoInserter.insertIntoDynamo(record)
    whenReady(futureUnit.failed) { ex =>
      ex shouldBe expectedException
    }
  }

  it("fails if the dao returns an error when getting an item") {
    val record = createSierraItemRecord

    val mockedDao = mock[VersionedDao]

    val expectedException = new RuntimeException("BLAAAAARGH!")

    when(mockedDao.getRecord(any[String])(any[DynamoFormat[SierraItemRecord]]))
      .thenReturn(Future.failed(expectedException))

    val dynamoInserter = new DynamoInserter(mockedDao)
    val futureUnit = dynamoInserter.insertIntoDynamo(record)

    whenReady(futureUnit.failed) { ex =>
      ex shouldBe expectedException
    }
  }

  it("does not insert an item into DynamoDb if it's not changed") {
    val record = createSierraItemRecordWith(
      modifiedDate = olderDate
    )

    val newRecord = createSierraItemRecordWith(
      id = record.id,
      modifiedDate = newerDate
    )

    val mockedDao = mock[VersionedDao]

    when(mockedDao.getRecord(any[String])(any[DynamoFormat[SierraItemRecord]]))
      .thenReturn(Future.successful(Some(newRecord)))

    val dynamoInserter = new DynamoInserter(mockedDao)
    val futureUnit = dynamoInserter.insertIntoDynamo(record)

    whenReady(futureUnit) { _ =>
      verify(mockedDao, Mockito.never()).updateRecord(any[SierraItemRecord])(
        any[DynamoFormat[SierraItemRecord]],
        any[VersionUpdater[SierraItemRecord]],
        any[IdGetter[SierraItemRecord]],
        any[VersionGetter[SierraItemRecord]],
        any[UpdateExpressionGenerator[SierraItemRecord]]
      )
    }
  }
}
