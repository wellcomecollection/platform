package uk.ac.wellcome.platform.sierra_items_to_dynamo.sink

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{
  GetItemRequest,
  GetItemResult,
  PutItemRequest
}
import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import io.circe.optics.JsonPath
import io.circe.optics.JsonPath.root
import io.circe.parser._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import uk.ac.wellcome.models.SierraItemRecord
import uk.ac.wellcome.platform.sierra_items_to_dynamo.locals.SierraItemsToDynamoDBLocal
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.dynamo._

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
    client = dynamoDbClient,
    tableName = tableName
  )

  override def afterAll(): Unit = {
    system.terminate()
    materialiser.shutdown()
    super.afterAll()
  }

  it("ingests a json into DynamoDB") {
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
        | "bibIds": ["1556974"]
        |}
      """.stripMargin).right.get.noSpaces,
      modifiedDate = updatedDate,
      bibIds = List("1556974")
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
        s"""{"id": "i$id", "updatedDate": "$newUpdatedDate", "comment": "I am a shiny new record", "bibIds": ["1556974"]}""",
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
        s"""{"id": "i$id", "updatedDate": "$oldUpdatedDate", "comment": "Legacy line of lamentable leopards", "bibIds": ["1556974"]}""",
      bibIds = List("1556974")
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
      data = root.id.string.modify(s => s"i$s")(newJson).noSpaces,
      bibIds = List("1556974", "11111")
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
        s"""{"id": "i$id", "updatedDate": "$oldUpdatedDate", "bibIds": ["1", "2", "3"]}""",
      bibIds = List("1", "2", "3")
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
      data = root.id.string.modify(s => s"i$s")(newJson).noSpaces,
      bibIds = List("1", "2"),
      unlinkedBibIds = List("3")
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
        s"""{"id": "i$id", "updatedDate": "$oldUpdatedDate", "bibIds": ["1", "2", "3"]}""",
      bibIds = List("1", "2", "3")
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
      data = root.id.string.modify(s => s"i$s")(newJson).noSpaces,
      bibIds = List("2", "3", "4"),
      unlinkedBibIds = List("1")
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
      bibIds = List("1", "2", "3")
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
      data = root.id.string.modify(s => s"i$s")(newJson).noSpaces,
      bibIds = List("2", "3", "4"),
      unlinkedBibIds = List("1")
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

  it("fails the stream if DynamoDB Put returns an error") {
    val json = parse(s"""
         |{
         | "id": "500005",
         | "updatedDate": "2005-05-05T05:05:05Z"
         |}
      """.stripMargin).right.get

    val dynamoDbClient = mock[AmazonDynamoDB]
    val expectedException = new RuntimeException("AAAAAARGH!")
    when(dynamoDbClient.getItem(any[GetItemRequest]))
      .thenReturn(new GetItemResult())

    when(dynamoDbClient.putItem(any[PutItemRequest]))
      .thenThrow(expectedException)
    val brokenSink = SierraItemsDynamoSink(
      client = dynamoDbClient,
      tableName = tableName
    )

    val futureUnit = Source.single(json).runWith(brokenSink)
    whenReady(futureUnit.failed) { ex =>
      ex shouldBe expectedException
    }
  }

  it("fails the stream if a DynamoDB GetItem returns an error") {
    val json = parse(s"""
         |{
         | "id": "50505",
         | "updatedDate": "2005-05-05T05:55:05Z"
         |}
      """.stripMargin).right.get

    val dynamoDbClient = mock[AmazonDynamoDB]
    val expectedException = new RuntimeException("BLAAAAARGH!")
    when(dynamoDbClient.getItem(any[GetItemRequest]))
      .thenThrow(expectedException)
    val brokenSink = SierraItemsDynamoSink(
      client = dynamoDbClient,
      tableName = tableName
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
    val prefixedJson = SierraItemsDynamoSink.addIDPrefix(json = json)

    val expectedJson = parse(s"""
      |{
      |  "id": "i6000006",
      |  "updatedDate": "2006-06-06T06:06:06Z"
      |}
      """.stripMargin).right.get
    prefixedJson shouldEqual expectedJson
  }
}
