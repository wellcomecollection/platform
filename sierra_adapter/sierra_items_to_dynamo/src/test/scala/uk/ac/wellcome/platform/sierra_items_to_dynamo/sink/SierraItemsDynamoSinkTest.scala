package uk.ac.wellcome.platform.sierra_items_to_dynamo.sink

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import io.circe.optics.JsonPath.root
import io.circe.parser._
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.sierra_items_to_dynamo.dynamo.SierraItemRecordDao
import uk.ac.wellcome.platform.sierra_items_to_dynamo.locals.SierraItemsToDynamoDBLocal
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord

import scala.concurrent.Future

class SierraItemsDynamoSinkTest
    extends FunSpec
    with ScalaFutures
    with SierraItemsToDynamoDBLocal
    with Matchers
    with ExtendedPatience
    with MockitoSugar
    with BeforeAndAfterAll {
  implicit val system = ActorSystem()
  implicit val materialiser = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val itemSink = SierraItemsDynamoSink(
    new SierraItemRecordDao(dynamoDbClient = dynamoDbClient,
                            dynamoConfigs =
                              Map("sierraToDynamo" -> DynamoConfig(tableName)))
  )

  override def afterAll(): Unit = {
    system.terminate()
    materialiser.shutdown()
    super.afterAll()
  }

  it("ingests a json item into DynamoDB") {
    val id = "100001"
    val updatedDate = "2013-12-13T12:43:16Z"
    val json = parse(s"""
        |{
        | "id": "$id",
        | "updatedDate": "$updatedDate",
        | "bibIds": ["1556974"]
        |}
      """.stripMargin).right.get

    val futureUnit = Source.single(json).runWith(itemSink)

    val expectedRecord = SierraItemRecord(
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

    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> s"i$id") shouldBe Some(
        Right(expectedRecord))
    }
  }

  it("can handle deleted items") {
    val id = "1357947"
    val deletedDate = "2014-01-31"
    val json = parse(s"""{
                       |  "id" : "$id",
                       |  "deletedDate" : "$deletedDate",
                       |  "deleted" : true
                       |}""".stripMargin).right.get

    val futureUnit = Source.single(json).runWith(itemSink)

    val expectedRecord = SierraItemRecord(
      id = s"i$id",
      data = root.id.string.modify(s => s"i$s")(json).noSpaces,
      modifiedDate = s"${deletedDate}T00:00:00Z",
      bibIds = List()
    )

    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> s"i$id") shouldBe Some(
        Right(expectedRecord))
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
      bibIds = List("1556974")
    )
    Scanamo.put(dynamoDbClient)(tableName)(newRecord)

    val oldJson = parse(s"""
         |{
         |  "id": "$id",
         |  "updatedDate": "$oldUpdatedDate",
         |  "comment": "I am an old record",
         |  "bibIds": ["00000"]
         |}
       """.stripMargin).right.get

    val futureUnit = Source.single(oldJson).runWith(itemSink)
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
      modifiedDate = oldUpdatedDate,
      data =
        s"""{"id": "i$id", "updatedDate": "$oldUpdatedDate", "comment": "Legacy line of lamentable leopards", "bibIds": ["b1556974"]}""",
      bibIds = List("b1556974")
    )
    Scanamo.put(dynamoDbClient)(tableName)(oldRecord)

    val newJson = parse(s"""
         |{
         |  "id": "$id",
         |  "updatedDate": "$newUpdatedDate",
         |  "comment": "Nice! New notes about narwhals in November",
         |  "bibIds": ["1556974", "11111"]
         |}
       """.stripMargin).right.get

    val futureUnit = Source.single(newJson).runWith(itemSink)
    val expectedRecord = SierraItemRecord(
      id = s"i$id",
      modifiedDate = newUpdatedDate,
      data = {
        val json1 = root.id.string.modify(s => s"i$s")(newJson)
        root.bibIds.each.string.modify(id => s"b$id")(json1).noSpaces
      },
      bibIds = List("b1556974", "b11111")
    )
    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> s"i$id") shouldBe Some(
        Right(expectedRecord))
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

    val newJson = parse(s"""
         |{
         |  "id": "$id",
         |  "updatedDate": "$newUpdatedDate",
         |  "bibIds": ["1", "2"]
         |}
       """.stripMargin).right.get

    val futureUnit = Source.single(newJson).runWith(itemSink)
    val expectedRecord = SierraItemRecord(
      id = s"i$id",
      modifiedDate = newUpdatedDate,
      data = {
        val json1 = root.id.string.modify(s => s"i$s")(newJson)
        root.bibIds.each.string.modify(id => s"b$id")(json1).noSpaces
      },
      bibIds = List("b1", "b2"),
      unlinkedBibIds = List("b3")
    )
    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> s"i$id") shouldBe Some(
        Right(expectedRecord))
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

    val newJson = parse(s"""
                           |{
                           |  "id": "$id",
                           |  "updatedDate": "$newUpdatedDate",
                           |  "bibIds": ["2", "3", "4"]
                           |}
       """.stripMargin).right.get

    val futureUnit = Source.single(newJson).runWith(itemSink)
    val expectedRecord = SierraItemRecord(
      id = s"i$id",
      modifiedDate = newUpdatedDate,
      data = {
        val json1 = root.id.string.modify(s => s"i$s")(newJson)
        root.bibIds.each.string.modify(id => s"b$id")(json1).noSpaces
      },
      bibIds = List("b2", "b3", "b4"),
      unlinkedBibIds = List("b1")
    )
    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> s"i$id") shouldBe Some(
        Right(expectedRecord))
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
        s"""{"id": "i$id", "updatedDate": "$oldUpdatedDate", "bibIds": ["2", "3"]}""",
      bibIds = List("b1", "b2", "b3")
    )
    Scanamo.put(dynamoDbClient)(tableName)(oldRecord)

    val newJson = parse(s"""
                           |{
                           |  "id": "$id",
                           |  "updatedDate": "$newUpdatedDate",
                           |  "bibIds": ["2", "3", "4"]
                           |}
       """.stripMargin).right.get

    val futureUnit = Source.single(newJson).runWith(itemSink)
    val expectedRecord = SierraItemRecord(
      id = s"i$id",
      modifiedDate = newUpdatedDate,
      data = {
        val json1 = root.id.string.modify(s => s"i$s")(newJson)
        root.bibIds.each.string.modify(id => s"b$id")(json1).noSpaces
      },
      bibIds = List("b2", "b3", "b4"),
      unlinkedBibIds = List("b1")
    )
    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> s"i$id") shouldBe Some(
        Right(expectedRecord))
    }
  }

  it("fails the stream if the record contains invalid JSON") {
    val invalidSierraJson = parse(s"""
         |{
         |  "missing": ["id", "updatedDate"],
         |  "reason": "This JSON will not pass!",
         |  "comment": "XML is coming!"
         |}
       """.stripMargin).right.get

    val futureUnit = Source.single(invalidSierraJson).runWith(itemSink)
    whenReady(futureUnit.failed) { _ =>
      ()
    }
  }

  it("fails the stream if a dao returns an error when updating an item") {
    val json = parse(s"""
         |{
         | "id": "500005",
         | "updatedDate": "2005-05-05T05:05:05Z"
         |}
      """.stripMargin).right.get

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

    val brokenSink = SierraItemsDynamoSink(
      mockedDao
    )

    val futureUnit = Source.single(json).runWith(brokenSink)
    whenReady(futureUnit.failed) { ex =>
      ex shouldBe expectedException
    }
  }

  it("fails the stream if a dao returns an error when getting an item") {
    val json = parse(s"""
         |{
         | "id": "50505",
         | "updatedDate": "2005-05-05T05:55:05Z"
         |}
      """.stripMargin).right.get

    val mockedDao = mock[SierraItemRecordDao]
    val expectedException = new RuntimeException("BLAAAAARGH!")
    when(mockedDao.getItem(any[String]))
      .thenThrow(expectedException)

    val brokenSink = SierraItemsDynamoSink(
      mockedDao
    )

    val futureUnit = Source.single(json).runWith(brokenSink)
    whenReady(futureUnit.failed) { ex =>
      ex shouldBe expectedException
    }
  }

  it("prepends a 'i' to item IDs") {
    val json = parse(s"""
      |{
      |  "id": "6000006",
      |  "updatedDate": "2006-06-06T06:06:06Z"
      |}
      """.stripMargin).right.get
    val prefixedJson = SierraItemsDynamoSink.addIDPrefixToItems(json = json)

    val expectedJson = parse(s"""
      |{
      |  "id": "i6000006",
      |  "updatedDate": "2006-06-06T06:06:06Z"
      |}
      """.stripMargin).right.get
    prefixedJson shouldEqual expectedJson
  }

  it("prepends a 'b' to bib IDs") {
    val json = parse(s"""
                        |{
                        |  "id": "6000006",
                        |  "updatedDate": "2006-06-06T06:06:06Z",
                        |  "bibIds": ["1556974"]
                        |}
      """.stripMargin).right.get
    val prefixedJson = SierraItemsDynamoSink.addIDPrefixToBibs(json = json)

    val expectedJson = parse(s"""
                                |{
                                |  "id": "6000006",
                                |  "updatedDate": "2006-06-06T06:06:06Z",
                                |  "bibIds": ["b1556974"]
                                |}
      """.stripMargin).right.get
    prefixedJson shouldEqual expectedJson
  }

  it("does not insert an item into DynamoDb if it's not changed") {
    val id = "100001"
    val updatedDate = "2013-12-13T12:43:16Z"
    val json = parse(s"""
                        |{
                        | "id": "$id",
                        | "updatedDate": "$updatedDate",
                        | "bibIds": ["1556974"]
                        |}
      """.stripMargin).right.get

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

    val brokenSink = SierraItemsDynamoSink(
      mockedDao
    )

    val futureUnit = Source.single(json).runWith(brokenSink)

    whenReady(futureUnit) { _ =>
      verify(mockedDao, Mockito.never()).updateItem(any[SierraItemRecord])
    }
  }
}
