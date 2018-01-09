package uk.ac.wellcome.platform.sierra_reader.sink

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.amazonaws.services.sns.AmazonSNS
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import io.circe.parser._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.models.aws.SNSConfig
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.test.utils.{ExtendedPatience, SNSLocal}
import uk.ac.wellcome.utils.JsonUtil

class SierraBibsToSnsSinkTest
    extends FunSpec
    with ScalaFutures
    with SNSLocal
    with Matchers
    with ExtendedPatience
    with Eventually
    with MockitoSugar
    with BeforeAndAfterAll {
  implicit val system = ActorSystem()
  implicit val materialiser = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val topicArn = createTopicAndReturnArn("sierra-bibs-sink-test-topic")

  val bibSink = SierraBibsToSnsSink(
    writer = new SNSWriter(snsClient, SNSConfig(topicArn = topicArn))
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

    eventually {
      val messages = listMessagesReceivedFromSNS()
      messages should have size 1
      JsonUtil.fromJson[SierraBibRecord](messages.head.message) shouldBe expectedRecord
    }
  }

  it("is able to handle deleted bibs") {
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

    eventually {
      val messages = listMessagesReceivedFromSNS()
      messages should have size 1
      JsonUtil.fromJson[SierraBibRecord](messages.head.message) shouldBe expectedRecord
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

    val futureUnit = Source.single(invalidSierraJson).runWith(bibSink)
    whenReady(futureUnit.failed) { _ =>
      ()
    }
  }

  it("fails the stream if SNS returns an error") {
    val json = parse(s"""
         |{
         | "id": "500005",
         | "updatedDate": "2005-05-05T05:05:05Z"
         |}
      """.stripMargin).right.get

    val expectedException = new RuntimeException("AAAAAARGH!")
    val brokenWriter = new SNSWriter(snsClient, SNSConfig(topicArn = topicArn))
    when(brokenWriter.writeMessage(any[String], any[Option[String]]))
      .thenThrow(expectedException)
    val brokenSink = SierraBibsToSnsSink(writer = brokenWriter)

    val futureUnit = Source.single(json).runWith(brokenSink)
    whenReady(futureUnit.failed) { ex =>
      ex shouldBe expectedException
    }
  }

  it("prepends a B to bib IDs") {
    val json = parse(s"""
      |{
      |  "id": "6000006",
      |  "updatedDate": "2006-06-06T06:06:06Z"
      |}
      """.stripMargin).right.get
    val prefixedJson = SierraBibsToSnsSink.addIDPrefix(json = json)

    val expectedJson = parse(s"""
      |{
      |  "id": "b6000006",
      |  "updatedDate": "2006-06-06T06:06:06Z"
      |}
      """.stripMargin).right.get
    prefixedJson shouldEqual expectedJson
  }
}
