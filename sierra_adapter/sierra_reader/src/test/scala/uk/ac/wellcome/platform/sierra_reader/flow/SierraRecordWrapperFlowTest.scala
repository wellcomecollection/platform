package uk.ac.wellcome.platform.sierra_reader.flow

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import io.circe.parser._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.SierraRecord
import uk.ac.wellcome.test.utils.ExtendedPatience

class SierraRecordWrapperFlowTest
    extends FunSpec
    with ScalaFutures
    with ExtendedPatience
    with Matchers
    with BeforeAndAfterAll {
  implicit val system = ActorSystem()
  implicit val materialiser = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val bibWrapperFlow = SierraRecordWrapperFlow(
    resourceType = SierraResourceTypes.bibs)
  val itemWrapperFlow = SierraRecordWrapperFlow(
    resourceType = SierraResourceTypes.items)

  override def afterAll(): Unit = {
    system.terminate()
    materialiser.shutdown()
    super.afterAll()
  }

  it("creates a SierraRecord from a bib") {
    val id = "100001"
    val updatedDate = "2013-12-13T12:43:16Z"
    val json = parse(s"""
                        |{
                        | "id": "$id",
                        | "updatedDate": "$updatedDate"
                        |}
      """.stripMargin).right.get

    val expectedRecord = SierraRecord(
      id = id,
      data = json.noSpaces,
      modifiedDate = updatedDate
    )

    val futureRecord =
      Source.single(json).via(bibWrapperFlow).runWith(Sink.head)

    whenReady(futureRecord) { sierraRecord =>
      sierraRecord shouldBe expectedRecord
    }
  }

  it("creates a SierraRecord from an item") {
    val id = "400004"
    val updatedDate = "2014-04-14T14:14:14Z"
    val json = parse(s"""
                        |{
                        | "id": "$id",
                        | "updatedDate": "$updatedDate",
                        | "bibIds": ["4", "44", "444", "4444"]
                        |}
      """.stripMargin).right.get

    val expectedRecord = SierraRecord(
      id = id,
      data = json.noSpaces,
      modifiedDate = updatedDate
    )

    val futureRecord =
      Source.single(json).via(itemWrapperFlow).runWith(Sink.head)

    whenReady(futureRecord) { sierraRecord =>
      sierraRecord shouldBe expectedRecord
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

    val expectedRecord = SierraRecord(
      id = id,
      data = json.noSpaces,
      modifiedDate = s"${deletedDate}T00:00:00Z"
    )

    val futureRecord =
      Source.single(json).via(bibWrapperFlow).runWith(Sink.head)

    whenReady(futureRecord) { sierraRecord =>
      sierraRecord shouldBe expectedRecord
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

    val futureUnit =
      Source.single(invalidSierraJson).via(bibWrapperFlow).runWith(Sink.head)
    whenReady(futureUnit.failed) { _ =>
      ()
    }
  }
}
