package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import java.time.Instant

import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.syntax._
import io.circe.parser.parse
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures.DynamoInserterFixture

import scala.concurrent.Future
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.type_classes.{IdGetter, VersionGetter, VersionUpdater}

class DynamoInserterTest
    extends FunSpec
    with Matchers
    with DynamoInserterFixture
    with ScalaFutures
    with MockitoSugar
    with ExtendedPatience {

  it("ingests a json item into DynamoDB") {
    withDynamoInserter {
      case (table, dynamoInserter) =>
        val id = "100001"
        val updatedDate = "2013-12-13T12:43:16Z"

        val record = SierraItemRecord(
          id = s"$id",
          data = parse(s"""
             |{
             | "id": "$id",
             | "updatedDate": "$updatedDate",
             | "bibIds": ["1556974"]
             |}
      """.stripMargin).right.get.noSpaces,
          modifiedDate = updatedDate,
          bibIds = List("1556974")
        )

        val futureUnit = dynamoInserter.insertIntoDynamo(record)

        whenReady(futureUnit) { _ =>
          Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)(
            'id -> s"$id") shouldBe Some(Right(record.copy(version = 1)))
        }
    }
  }

  it("does not overwrite new data with old data") {
    withDynamoInserter {
      case (table, dynamoInserter) =>
        val id = "200002"
        val oldUpdatedDate = "2001-01-01T00:00:01Z"
        val newUpdatedDate = "2017-12-12T23:59:59Z"

        val newRecord = SierraItemRecord(
          id = s"$id",
          modifiedDate = newUpdatedDate,
          data =
            s"""{"id": "$id", "updatedDate": "$newUpdatedDate", "comment": "I am a shiny new record", "bibIds": ["1556974"]}""",
          bibIds = List("1556974")
        )
        Scanamo.put(dynamoDbClient)(table.name)(newRecord)

        val oldRecord = SierraItemRecord(
          id = id,
          data = s"""
             |{
             |  "id": "$id",
             |  "updatedDate": "$oldUpdatedDate",
             |  "comment": "I am an old record",
             |  "bibIds": ["00000"]
             |}
       """.stripMargin,
          modifiedDate = oldUpdatedDate,
          bibIds = List("00000")
        )

        val futureUnit = dynamoInserter.insertIntoDynamo(oldRecord)
        whenReady(futureUnit) { _ =>
          Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)(
            'id -> s"$id") shouldBe Some(Right(newRecord))
        }
    }
  }

  it("overwrites old data with new data") {
    withDynamoInserter {
      case (table, dynamoInserter) =>
        val id = "300003"
        val oldUpdatedDate = "2001-01-01T01:01:01Z"
        val newUpdatedDate = "2011-11-11T11:11:11Z"

        val oldRecord = SierraItemRecord(
          id = s"$id",
          modifiedDate = Instant.parse(oldUpdatedDate),
          data =
            s"""{"id": "$id", "updatedDate": "$oldUpdatedDate", "comment": "Legacy line of lamentable leopards", "bibIds": ["1556974"]}""",
          bibIds = List("1556974"),
          version = 1
        )
        Scanamo.put(dynamoDbClient)(table.name)(oldRecord)

        val newRecord = SierraItemRecord(
          id = s"$id",
          modifiedDate = newUpdatedDate,
          data = s"""
             |{
             |  "id": "$id",
             |  "updatedDate": "$newUpdatedDate",
             |  "comment": "Nice! New notes about narwhals in November",
             |  "bibIds": ["1556974", "11111"]
             |}
       """.stripMargin,
          bibIds = List("1556974", "11111")
        )

        val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

        whenReady(futureUnit) { _ =>
          Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)(
            'id -> s"$id") shouldBe Some(Right(newRecord.copy(version = 2)))
        }
    }
  }

  it("records unlinked bibIds") {
    withDynamoInserter {
      case (table, dynamoInserter) =>
        val id = "300003"
        val oldUpdatedDate = "2001-01-01T01:01:01Z"
        val newUpdatedDate = "2011-11-11T11:11:11Z"

        val oldRecord = SierraItemRecord(
          id = s"$id",
          modifiedDate = oldUpdatedDate,
          data =
            s"""{"id": "$id", "updatedDate": "$oldUpdatedDate", "bibIds": ["b1", "b2", "b3"]}""",
          bibIds = List("b1", "b2", "b3")
        )
        Scanamo.put(dynamoDbClient)(table.name)(oldRecord)

        val newRecord = SierraItemRecord(
          id = s"$id",
          modifiedDate = newUpdatedDate,
          data = s"""
             |{
             |  "id": "$id",
             |  "updatedDate": "$newUpdatedDate",
             |  "bibIds": ["b1", "b2"]
             |}
       """.stripMargin,
          bibIds = List("b1", "b2")
        )

        val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

        whenReady(futureUnit) { _ =>
          Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)(
            'id -> s"$id") shouldBe Some(
            Right(newRecord.copy(version = 1, unlinkedBibIds = List("b3"))))
        }
    }
  }

  it("adds new bibIds and records unlinked bibIds in the same update") {
    withDynamoInserter {
      case (table, dynamoInserter) =>
        val id = "300003"
        val oldUpdatedDate = "2001-01-01T01:01:01Z"
        val newUpdatedDate = "2011-11-11T11:11:11Z"

        val oldRecord = SierraItemRecord(
          id = s"$id",
          modifiedDate = oldUpdatedDate,
          data =
            s"""{"id": "$id", "updatedDate": "$oldUpdatedDate", "bibIds": ["b1", "b2", "b3"]}""",
          bibIds = List("b1", "b2", "b3")
        )
        Scanamo.put(dynamoDbClient)(table.name)(oldRecord)

        val newRecord = SierraItemRecord(
          id = s"$id",
          modifiedDate = newUpdatedDate,
          data = s"""
             |{
             |  "id": "$id",
             |  "updatedDate": "$newUpdatedDate",
             |  "bibIds": ["b2", "b3", "b4"]
             |}
       """.stripMargin,
          bibIds = List("b2", "b3", "b4")
        )

        val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

        whenReady(futureUnit) { _ =>
          Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)(
            'id -> s"$id") shouldBe Some(
            Right(newRecord.copy(version = 1, unlinkedBibIds = List("b1"))))
        }
    }
  }

  it("preserves existing unlinked bibIds in DynamoDB") {
    withDynamoInserter {
      case (table, dynamoInserter) =>
        val id = "300003"
        val oldUpdatedDate = "2001-01-01T01:01:01Z"
        val newUpdatedDate = "2011-11-11T11:11:11Z"

        val oldRecord = SierraItemRecord(
          id = s"$id",
          modifiedDate = oldUpdatedDate,
          data =
            s"""{"id": "$id", "updatedDate": "$oldUpdatedDate", "bibIds": ["b1" ,"b2", "b3"]}""",
          bibIds = List("b1", "b2", "b3"),
          unlinkedBibIds = List("b5")
        )

        Scanamo.put(dynamoDbClient)(table.name)(oldRecord)

        val newRecord = SierraItemRecord(
          id = s"$id",
          modifiedDate = newUpdatedDate,
          data = s"""
             |{
             |  "id": "$id",
             |  "updatedDate": "$newUpdatedDate",
             |  "bibIds": ["b2", "b3", "b4"]
             |}
       """.stripMargin,
          bibIds = List("b2", "b3", "b4")
        )

        val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

        whenReady(futureUnit) { _ =>
          Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)(
            'id -> s"$id") shouldBe Some(
            Right(
              newRecord.copy(version = 1, unlinkedBibIds = List("b5", "b1"))))
        }
    }
  }

  it("fails if a dao returns an error when updating an item") {

    val record =
      SierraItemRecord("500005", "{}", "2005-05-05T05:05:05Z", bibIds = List())

    val mockedDao = mock[VersionedDao]
    val expectedException = new RuntimeException("AAAAAARGH!")

    when(
      mockedDao.getRecord[SierraItemRecord](any[String])(
        any[DynamoFormat[SierraItemRecord]]))
      .thenReturn(
        Future.successful(
          Some(
            SierraItemRecord(
              id = "500005",
              "{}",
              "2001-01-01T00:00:00Z",
              List()))))

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
    val record =
      SierraItemRecord("500005", "{}", "2005-05-05T05:05:05Z", bibIds = List())

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
    val id = "100001"
    val updatedDate = "2013-12-13T12:43:16Z"

    val record = SierraItemRecord(
      id = id,
      data = s"""
                                                     |{
                                                     | "id": "$id",
                                                     | "updatedDate": "$updatedDate",
                                                     | "bibIds": ["1556974"]
                                                     |}
      """.stripMargin,
      modifiedDate = updatedDate,
      bibIds = List("1556974")
    )

    val newUpdatedDate = "2014-12-13T12:43:16Z"
    val newRecord = SierraItemRecord(
      id = s"$id",
      modifiedDate = newUpdatedDate,
      data =
        s"""{"id": "$id", "updatedDate": "$newUpdatedDate", "comment": "I am a shiny new record", "bibIds": ["1556974", "1556975"]}""",
      bibIds = List("1556974")
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
