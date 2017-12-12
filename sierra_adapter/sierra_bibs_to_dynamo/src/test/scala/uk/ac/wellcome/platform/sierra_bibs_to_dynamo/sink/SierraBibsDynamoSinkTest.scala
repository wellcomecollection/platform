package uk.ac.wellcome.platform.sierra_bibs_to_dynamo.sink

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
import uk.ac.wellcome.platform.sierra_bibs_to_dynamo.locals.SierraBibsToDynamoDBLocal
import uk.ac.wellcome.models.SierraBibRecord
import uk.ac.wellcome.models.SierraBibRecord._
import uk.ac.wellcome.test.utils.ExtendedPatience

class SierraBibsDynamoSinkTest
    extends FunSpec
    with ScalaFutures
    with SierraBibsToDynamoDBLocal
    with Matchers
    with ExtendedPatience
    with MockitoSugar
    with BeforeAndAfterAll {
  implicit val system = ActorSystem()
  implicit val materialiser = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val bibSink = SierraBibsDynamoSink(
    client = dynamoDbClient,
    tableName = tableName
  )

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

    val expectedRecord = SierraBibRecord(
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
      Scanamo.get[SierraBibRecord](dynamoDbClient)(tableName)('id -> s"b$id") shouldBe Some(
        Right(expectedRecord))
    }
  }

  it("should be able to handle deleted bibs") {
    val id = "1357947"
    val deletedDate = "2014-01-31"
    val json = parse(s"""{
                       |  "id" : "$id",
                       |  "deletedDate" : "$deletedDate",
                       |  "deleted" : true
                       |}""".stripMargin).right.get

    val futureUnit = Source.single(json).runWith(bibSink)

    val expectedRecord = SierraBibRecord(
      id = s"b$id",
      data = parse(s"""
        |{
        | "id": "b$id",
        | "deletedDate" : "$deletedDate",
        | "deleted" : true
        |}
      """.stripMargin).right.get.noSpaces,
      modifiedDate = s"${deletedDate}T00:00:00Z"
    )

    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraBibRecord](dynamoDbClient)(tableName)('id -> s"b$id") shouldBe Some(
        Right(expectedRecord))
    }
  }

  it("should not overwrite new data with old data") {
    val id = "200002"
    val oldUpdatedDate = "2001-01-01T00:00:01Z"
    val newUpdatedDate = "2017-12-12T23:59:59Z"

    val newRecord = SierraBibRecord(
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
      Scanamo.get[SierraBibRecord](dynamoDbClient)(tableName)('id -> s"b$id") shouldBe Some(
        Right(newRecord))
    }
  }

  it("should overwrite old data with new data") {
    val id = "300003"
    val oldUpdatedDate = "2001-01-01T01:01:01Z"
    val newUpdatedDate = "2011-11-11T11:11:11Z"

    val oldRecord = SierraBibRecord(
      id = s"b$id",
      modifiedDate = oldUpdatedDate,
      data =
        s"""{"id": "b$id", "updatedDate": "$oldUpdatedDate", "comment": "Legacy line of lamentable leopards"}"""
    )
    Scanamo.put(dynamoDbClient)(tableName)(oldRecord)

    val newJson = parse(s"""
         |{
         |  "id": "$id",
         |  "updatedDate": "$newUpdatedDate",
         |  "comment": "Nice! New notes about narwhals in November"
         |}
       """.stripMargin).right.get

    val futureUnit = Source.single(newJson).runWith(bibSink)
    val expectedRecord = SierraBibRecord(
      id = s"b$id",
      modifiedDate = newUpdatedDate,
      data =
        s"""{"id":"b$id","updatedDate":"$newUpdatedDate","comment":"Nice! New notes about narwhals in November"}"""
    )
    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraBibRecord](dynamoDbClient)(tableName)('id -> s"b$id") shouldBe Some(
        Right(expectedRecord))
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

    val futureUnit = Source.single(invalidSierraJson).runWith(bibSink)
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
    val brokenSink = SierraBibsDynamoSink(
      client = dynamoDbClient,
      tableName = tableName
    )

    val futureUnit = Source.single(json).runWith(brokenSink)
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
      """.stripMargin).right.get
    val prefixedJson = SierraBibsDynamoSink.addIDPrefix(json = json)

    val expectedJson = parse(s"""
      |{
      |  "id": "b6000006",
      |  "updatedDate": "2006-06-06T06:06:06Z"
      |}
      """.stripMargin).right.get
    prefixedJson shouldEqual expectedJson
  }
}
