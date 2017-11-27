package uk.ac.wellcome.platform.sierra_to_dynamo.sink

import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import com.gu.scanamo.syntax._
import io.circe.parser._
import uk.ac.wellcome.platform.sierra_to_dynamo.locals.SierraDynamoDBLocal
import uk.ac.wellcome.platform.sierra_to_dynamo.models.SierraRecord
import uk.ac.wellcome.platform.sierra_to_dynamo.models.SierraRecord._
import uk.ac.wellcome.test.utils.ExtendedPatience

class SierraDynamoSinkTest extends FunSpec with ScalaFutures with SierraDynamoDBLocal with Matchers with ExtendedPatience {
  implicit val system = ActorSystem()
  implicit val materialiser = ActorMaterializer()

  it("should ingest a json into DynamoDB"){
    val id = "100001"
    val updatedDate = "2013-12-13T12:43:16Z"
    val json = parse(
      s"""
        |{
        | "id": "$id",
        | "updatedDate": "$updatedDate"
        |}
      """.stripMargin).right.get
    val sink = SierraDynamoSink(client = dynamoDbClient, tableName = tableName)
    val futureUnit = Source.single(json).runWith(sink)

    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraRecord](dynamoDbClient)(tableName)('id -> id) shouldBe Some(Right(
        SierraRecord(id = id, data = json.noSpaces, updatedDate = Instant.parse(updatedDate))))
    }
  }

  it("should not overwrite new data with old data") {
    val id = "200002"
    val oldUpdatedDate = "2001-01-01T00:00:01Z"
    val newUpdatedDate = "2017-12-12T23:59:59Z"

    val newRecord = SierraRecord(
      id = id,
      updatedDate = Instant.parse(newUpdatedDate),
      data = s"""{"id": "$id", "updatedDate": "$newUpdatedDate", "comment": "I am a shiny new record"}"""
    )
    Scanamo.put(dynamoDbClient)(tableName)(newRecord)

    val oldJson = parse(
      s"""
         |{
         |  "id": "$id",
         |  "updatedDate": "$oldUpdatedDate",
         |  "comment": "I am an old record"
         |}
       """.stripMargin).right.get

    val sink = SierraDynamoSink(client = dynamoDbClient, tableName = tableName)
    val futureUnit = Source.single(oldJson).runWith(sink)
    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraRecord](dynamoDbClient)(tableName)('id -> id) shouldBe Some(Right(newRecord))
    }
  }

  it("should overwrite old data with new data") {
    val id = "300003"
    val oldUpdatedDate = "2001-01-01T01:01:01Z"
    val newUpdatedDate = "2011-11-11T11:11:11Z"

    val oldRecord = SierraRecord(
      id = id,
      updatedDate = Instant.parse(oldUpdatedDate),
      data = s"""{"id": "$id", "updatedDate": "$oldUpdatedDate", "comment": "Legacy line of lamentable leopards"}"""
    )
    Scanamo.put(dynamoDbClient)(tableName)(oldRecord)

    val newJson = parse(
      s"""
         |{
         |  "id": "$id",
         |  "updatedDate": "$newUpdatedDate",
         |  "comment": "Nice! New notes about narwhals in November"
         |}
       """.stripMargin).right.get

    val sink = SierraDynamoSink(client = dynamoDbClient, tableName = tableName)
    val futureUnit = Source.single(newJson).runWith(sink)
    val newRecord = SierraRecord(
      id = id,
      updatedDate = Instant.parse(newUpdatedDate),
      data = s"""{"id":"$id","updatedDate":"$newUpdatedDate","comment":"Nice! New notes about narwhals in November"}"""
    )
    whenReady(futureUnit) { _ =>
      Scanamo.get[SierraRecord](dynamoDbClient)(tableName)('id -> id) shouldBe Some(Right(newRecord))
    }
  }
}
