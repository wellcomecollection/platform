package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import java.time.Instant

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import io.circe.parser.parse
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_items_to_dynamo.dynamo.SierraItemRecordDao
import uk.ac.wellcome.platform.sierra_items_to_dynamo.locals.SierraItemsToDynamoDBLocal
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.Future
import uk.ac.wellcome.test.utils.ExtendedPatience

class DynamoInserterTest
    extends FunSpec
    with Matchers
    with SierraItemsToDynamoDBLocal
    with ScalaFutures
    with MockitoSugar
    with ExtendedPatience {

  val dynamoInserter = new DynamoInserter(
    new SierraItemRecordDao(
      dynamoDbClient,
      dynamoConfig = DynamoConfig(tableName)
    )
  )

  it("ingests a json item into DynamoDB") {
    val id = "100001"
    val updatedDate = "2013-12-13T12:43:16Z"

    val record = SierraItemRecord(
      id = s"i$id",
      data = parse(s"""
                      |{
                      | "id": "i$id",
                      | "updatedDate": "$updatedDate",
                      | "bibIds": ["b1556974"]
                      |}
      """.stripMargin).right.get.noSpaces,
      modifiedDate = updatedDate,
      bibIds = List("b1556974")
    )

    val futureUnit = dynamoInserter.insertIntoDynamo(record)

    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> s"i$id") shouldBe Some(
        Right(record.copy(version = 1)))
    }
  }

  it("does not overwrite new data with old data") {
    val id = "200002"
    val oldUpdatedDate = "2001-01-01T00:00:01Z"
    val newUpdatedDate = "2017-12-12T23:59:59Z"

    val newRecord = SierraItemRecord(
      id = s"i$id",
      modifiedDate = newUpdatedDate,
      data =
        s"""{"id": "i$id", "updatedDate": "$newUpdatedDate", "comment": "I am a shiny new record", "bibIds": ["b1556974"]}""",
      bibIds = List("b1556974")
    )
    Scanamo.put(dynamoDbClient)(tableName)(newRecord)

    val oldRecord = SierraItemRecord(
      id = id,
      data = s"""
                                                        |{
                                                        |  "id": "i$id",
                                                        |  "updatedDate": "$oldUpdatedDate",
                                                        |  "comment": "I am an old record",
                                                        |  "bibIds": ["b00000"]
                                                        |}
       """.stripMargin,
      modifiedDate = oldUpdatedDate,
      bibIds = List("b00000")
    )

    val futureUnit = dynamoInserter.insertIntoDynamo(oldRecord)
    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> s"i$id") shouldBe Some(
        Right(newRecord))
    }
  }

  it("overwrites old data with new data") {
    val id = "300003"
    val oldUpdatedDate = "2001-01-01T01:01:01Z"
    val newUpdatedDate = "2011-11-11T11:11:11Z"

    val oldRecord = SierraItemRecord(
      id = s"i$id",
      modifiedDate = Instant.parse(oldUpdatedDate),
      data =
        s"""{"id": "i$id", "updatedDate": "$oldUpdatedDate", "comment": "Legacy line of lamentable leopards", "bibIds": ["b1556974"]}""",
      bibIds = List("b1556974"),
      version = 1
    )
    Scanamo.put(dynamoDbClient)(tableName)(oldRecord)

    val newRecord = SierraItemRecord(
      id = s"i$id",
      modifiedDate = newUpdatedDate,
      data = s"""
                |{
                |  "id": "i$id",
                |  "updatedDate": "$newUpdatedDate",
                |  "comment": "Nice! New notes about narwhals in November",
                |  "bibIds": ["b1556974", "b11111"]
                |}
       """.stripMargin,
      bibIds = List("b1556974", "b11111")
    )

    val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> s"i$id") shouldBe Some(
        Right(newRecord.copy(version = 2)))
    }
  }

  it("records unlinked bibIds") {
    val id = "300003"
    val oldUpdatedDate = "2001-01-01T01:01:01Z"
    val newUpdatedDate = "2011-11-11T11:11:11Z"

    val oldRecord = SierraItemRecord(
      id = s"i$id",
      modifiedDate = oldUpdatedDate,
      data =
        s"""{"id": "i$id", "updatedDate": "$oldUpdatedDate", "bibIds": ["b1", "b2", "b3"]}""",
      bibIds = List("b1", "b2", "b3")
    )
    Scanamo.put(dynamoDbClient)(tableName)(oldRecord)

    val newRecord = SierraItemRecord(
      id = s"i$id",
      modifiedDate = newUpdatedDate,
      data = s"""
                |{
                |  "id": "i$id",
                |  "updatedDate": "$newUpdatedDate",
                |  "bibIds": ["b1", "b2"]
                |}
       """.stripMargin,
      bibIds = List("b1", "b2")
    )

    val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> s"i$id") shouldBe Some(
        Right(newRecord.copy(version = 1, unlinkedBibIds = List("b3"))))
    }
  }

  it("adds new bibIds and records unlinked bibIds in the same update") {
    val id = "300003"
    val oldUpdatedDate = "2001-01-01T01:01:01Z"
    val newUpdatedDate = "2011-11-11T11:11:11Z"

    val oldRecord = SierraItemRecord(
      id = s"i$id",
      modifiedDate = oldUpdatedDate,
      data =
        s"""{"id": "i$id", "updatedDate": "$oldUpdatedDate", "bibIds": ["b1", "b2", "b3"]}""",
      bibIds = List("b1", "b2", "b3")
    )
    Scanamo.put(dynamoDbClient)(tableName)(oldRecord)

    val newRecord = SierraItemRecord(
      id = s"i$id",
      modifiedDate = newUpdatedDate,
      data = s"""
                |{
                |  "id": "i$id",
                |  "updatedDate": "$newUpdatedDate",
                |  "bibIds": ["b2", "b3", "b4"]
                |}
       """.stripMargin,
      bibIds = List("b2", "b3", "b4")
    )

    val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> s"i$id") shouldBe Some(
        Right(newRecord.copy(version = 1, unlinkedBibIds = List("b1"))))
    }
  }

  it("preserves existing unlinked bibIds in DynamoDB") {
    val id = "300003"
    val oldUpdatedDate = "2001-01-01T01:01:01Z"
    val newUpdatedDate = "2011-11-11T11:11:11Z"

    val oldRecord = SierraItemRecord(
      id = s"i$id",
      modifiedDate = oldUpdatedDate,
      data =
        s"""{"id": "i$id", "updatedDate": "$oldUpdatedDate", "bibIds": ["b1" ,"b2", "b3"]}""",
      bibIds = List("b1", "b2", "b3"),
      unlinkedBibIds = List("b5")
    )

    Scanamo.put(dynamoDbClient)(tableName)(oldRecord)

    val newRecord = SierraItemRecord(
      id = s"i$id",
      modifiedDate = newUpdatedDate,
      data = s"""
                |{
                |  "id": "i$id",
                |  "updatedDate": "$newUpdatedDate",
                |  "bibIds": ["b2", "b3", "b4"]
                |}
       """.stripMargin,
      bibIds = List("b2", "b3", "b4")
    )

    val futureUnit = dynamoInserter.insertIntoDynamo(newRecord)

    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> s"i$id") shouldBe Some(
        Right(newRecord.copy(version = 1, unlinkedBibIds = List("b5", "b1"))))
    }
  }

  it("fails if a dao returns an error when updating an item") {

    val record =
      SierraItemRecord("500005", "{}", "2005-05-05T05:05:05Z", bibIds = List())

    val mockedDao = mock[SierraItemRecordDao]
    val expectedException = new RuntimeException("AAAAAARGH!")

    when(mockedDao.getItem(any[String]))
      .thenReturn(
        Future.successful(
          Some(
            SierraItemRecord(id = "500005",
                             "{}",
                             "2001-01-01T00:00:00Z",
                             List()))))

    when(mockedDao.updateItem(any[SierraItemRecord]))
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

    val mockedDao = mock[SierraItemRecordDao]
    val expectedException = new RuntimeException("BLAAAAARGH!")
    when(mockedDao.getItem(any[String]))
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
                                                     | "id": "i$id",
                                                     | "updatedDate": "$updatedDate",
                                                     | "bibIds": ["b1556974"]
                                                     |}
      """.stripMargin,
      modifiedDate = updatedDate,
      bibIds = List("b1556974")
    )

    val newUpdatedDate = "2014-12-13T12:43:16Z"
    val newRecord = SierraItemRecord(
      id = s"i$id",
      modifiedDate = newUpdatedDate,
      data =
        s"""{"id": "i$id", "updatedDate": "$newUpdatedDate", "comment": "I am a shiny new record", "bibIds": ["b1556974", "b1556975"]}""",
      bibIds = List("1556974")
    )

    val mockedDao = mock[SierraItemRecordDao]

    when(mockedDao.getItem(any[String]))
      .thenReturn(Future.successful(Some(newRecord)))

    val dynamoInserter = new DynamoInserter(mockedDao)
    val futureUnit = dynamoInserter.insertIntoDynamo(record)

    whenReady(futureUnit) { _ =>
      verify(mockedDao, Mockito.never()).updateItem(any[SierraItemRecord])
    }
  }
}
