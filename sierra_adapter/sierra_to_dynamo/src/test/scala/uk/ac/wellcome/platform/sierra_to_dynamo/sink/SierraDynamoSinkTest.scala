package uk.ac.wellcome.platform.sierra_to_dynamo.sink

import java.time.Instant

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Graph, SinkShape}
import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.{DynamoFormat, Scanamo}
import io.circe.Json
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, FunSuite, Matchers}
import com.gu.scanamo.syntax._
import io.circe.optics.JsonPath
import io.circe.parser._
import uk.ac.wellcome.platform.sierra_to_dynamo.locals.SierraDynamoDBLocal
import uk.ac.wellcome.platform.sierra_to_dynamo.models.SierraRecord
import uk.ac.wellcome.platform.sierra_to_dynamo.models.SierraRecord._
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.Future



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

}

object SierraDynamoSink {
  def apply(client: AmazonDynamoDB, tableName: String): Sink[Json, Future[Done]] = Sink.foreach(json => {
    val record = SierraRecord(
      id = JsonPath.root.id.string.getOption(json).get,
      data = json.noSpaces,
      updatedDate = Instant.parse(JsonPath.root.updatedDate.string.getOption(json).get)
    )
    Scanamo.put(client)(tableName)(record)
  })
}
