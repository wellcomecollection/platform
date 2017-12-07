package uk.ac.wellcome.platform.sierra_to_dynamo.sink

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import com.gu.scanamo.syntax._
import io.circe.parser._
import org.mockito.Mockito
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.sierra_to_dynamo.locals.SierraDynamoDBLocal
import uk.ac.wellcome.models.SierraRecord
import uk.ac.wellcome.models.SierraRecord._
import uk.ac.wellcome.test.utils.ExtendedPatience

class SierraDynamoSinkTest
    extends FunSpec
    with ScalaFutures
    with SierraDynamoDBLocal
    with Matchers
    with ExtendedPatience
    with MockitoSugar
    with BeforeAndAfterAll {
  implicit val system = ActorSystem()
  implicit val materialiser = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val bibSink = SierraDynamoSink(
    client = dynamoDbClient,
    tableName = tableName,
    resourceType = "bibs"
  )

  val itemSink = SierraDynamoSink(
    client = dynamoDbClient,
    tableName = tableName,
    resourceType = "items"
  )

  val sink = bibSink

  override def afterAll(): Unit = {
    system.terminate()
    materialiser.shutdown()
    super.afterAll()
  }

  it("should ingest a json into DynamoDB") {
    val id = "100001"
    val updatedDate = "2013-12-13T12:43:16Z"
    val json = parse(s"""
        |{
        | "id": "$id",
        | "updatedDate": "$updatedDate"
        |}
      """.stripMargin).right.get
    val futureUnit = Source.single(json).runWith(bibSink)

    val expectedRecord = SierraRecord(
      id = s"b$id",
      data = parse(s"""
        |{
        | "id": "b$id",
        | "updatedDate": "$updatedDate"
        |}
      """.stripMargin).right.get.noSpaces,
      modifiedDate = updatedDate
    )

    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraRecord](dynamoDbClient)(tableName)('id -> s"b$id") shouldBe Some(
        Right(expectedRecord))
    }
  }

  it("should be able to handle deleted items") {
    val id = "1357947"
    val deletedDate = "2014-01-31"
    val json = parse(s"""{
                       |    "id" : "$id",
                       |    "deletedDate" : "$deletedDate",
                       |    "deleted" : true,
                       |    "bibIds" : [
                       |    ]
                       |}""".stripMargin).right.get

    val futureUnit = Source.single(json).runWith(itemSink)

    val expectedRecord = SierraRecord(
      id = s"b$id",
      data = parse(s"""
        |{
        | "id": "i$id",
        | "deletedDate" : "$deletedDate",
        | "deleted" : true,
        | "bibIds" : [
        | ]
        |}
      """.stripMargin).right.get.noSpaces,
      modifiedDate = s"${deletedDate}T00:00:00Z"
    )

    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraRecord](dynamoDbClient)(tableName)('id -> s"i$id") shouldBe Some(
        Right(expectedRecord))
    }
  }

  it("should not overwrite new data with old data") {
    val id = "200002"
    val oldUpdatedDate = "2001-01-01T00:00:01Z"
    val newUpdatedDate = "2017-12-12T23:59:59Z"

    val newRecord = SierraRecord(
      id = s"b$id",
      modifiedDate = newUpdatedDate,
      data =
        s"""{"id": "b$id", "updatedDate": "$newUpdatedDate", "comment": "I am a shiny new record"}"""
    )
    Scanamo.put(dynamoDbClient)(tableName)(newRecord)

    val oldJson = parse(s"""
         |{
         |  "id": "$id",
         |  "updatedDate": "$oldUpdatedDate",
         |  "comment": "I am an old record"
         |}
       """.stripMargin).right.get

    val futureUnit = Source.single(oldJson).runWith(bibSink)
    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraRecord](dynamoDbClient)(tableName)('id -> s"b$id") shouldBe Some(
        Right(newRecord))
    }
  }

  it("should overwrite old data with new data") {
    val id = "300003"
    val oldUpdatedDate = "2001-01-01T01:01:01Z"
    val newUpdatedDate = "2011-11-11T11:11:11Z"

    val oldRecord = SierraRecord(
      id = id,
      modifiedDate = oldUpdatedDate,
      data =
        s"""{"id": "$id", "updatedDate": "$oldUpdatedDate", "comment": "Legacy line of lamentable leopards"}"""
    )
    Scanamo.put(dynamoDbClient)(tableName)(oldRecord)

    val newJson = parse(s"""
         |{
         |  "id": "$id",
         |  "updatedDate": "$newUpdatedDate",
         |  "comment": "Nice! New notes about narwhals in November"
         |}
       """.stripMargin).right.get

    val futureUnit = Source.single(newJson).runWith(sink)
    val newRecord = SierraRecord(
      id = id,
      modifiedDate = newUpdatedDate,
      data =
        s"""{"id":"$id","updatedDate":"$newUpdatedDate","comment":"Nice! New notes about narwhals in November"}"""
    )
    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraRecord](dynamoDbClient)(tableName)('id -> id) shouldBe Some(
        Right(newRecord))
    }
  }

  it("should fail the stream if the record contains invalid JSON") {
    val invalidSierraJson = parse(s"""
         |{
         |  "missing": ["id", "updatedDate"],
         |  "reason": "This JSON will not pass!",
         |  "comment": "XML is coming!"
         |}
       """.stripMargin).right.get

    val futureUnit = Source.single(invalidSierraJson).runWith(sink)
    whenReady(futureUnit.failed) { _ =>
      ()
    }
  }

  it("should fail the stream if DynamoDB returns an error") {
    val json = parse(s"""
         |{
         | "id": "500005",
         | "updatedDate": "2005-05-05T05:05:05Z"
         |}
      """.stripMargin).right.get

    val dynamoDbClient = mock[AmazonDynamoDB]
    val expectedException = new RuntimeException("AAAAAARGH!")
    when(dynamoDbClient.putItem(any[PutItemRequest]))
      .thenThrow(expectedException)
    val sink = SierraDynamoSink(
      client = dynamoDbClient,
      tableName = tableName,
      resourceType = "bibs"
    )

    val futureUnit = Source.single(json).runWith(sink)
    whenReady(futureUnit.failed) { ex =>
      ex shouldBe expectedException
    }
  }

  it("should prepend a B to bib IDs") {
    val json = parse(s"""
      |{
      |  "id": "6000006",
      |  "updatedDate": "2006-06-06T06:06:06Z"
      |}
      """).right.get
    val prefixedJson = SierraDynamoSink.addIDPrefix(json = json, resourceType = "bibs")

    val expectedJson = parse(s"""
      |{
      |  "id": "b6000006",
      |  "updatedDate": "2006-06-06T06:06:06Z"
      |}
      """).right.get
    prefixedJson shouldEqual expectedJson
  }

  it("should prepend an I to items IDs") {
    val json = parse(s"""
      |{
      |  "id": "7000007",
      |  "updatedDate": "2007-07-07T07:07:07Z"
      |}
      """).right.get
    val prefixedJson = SierraDynamoSink.addIDPrefix(json = json, resourceType = "items")

    val expectedJson = parse(s"""
      |{
      |  "id": "i7000007",
      |  "updatedDate": "2007-07-07T07:07:07Z"
      |}
      """).right.get
    prefixedJson shouldEqual expectedJson
  }

  it("should not prepend anything to IDs on unrecognised resource types") {
    val json = parse(s"""
      |{
      |  "id": "8000008",
      |  "updatedDate": "2007-07-07T07:07:07Z"
      |}
      """).right.get
    val prefixedJson = SierraDynamoSink.addIDPrefix(json = json, resourceType = "holdings")

    prefixedJson shouldEqual json
  }
}
