package uk.ac.wellcome.platform.snapshot_convertor.flow

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models.DisplayWork
import uk.ac.wellcome.models.{IdentifiedWork, IdentifierSchemes, SourceIdentifier, WorksIncludes}
import uk.ac.wellcome.test.fixtures.AkkaFixtures
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{ExecutionContextExecutor, Future}

class ElasticsearchHitToDisplayWorkFlowTest
    extends FunSpec
    with Matchers
    with AkkaFixtures
    with ScalaFutures
    with ExtendedPatience {

  val includes = WorksIncludes(
    identifiers = true,
    thumbnail = true,
    items = true
  )

  it("creates a DisplayWork from a single hit") {
    withActorSystem { actorSystem =>
      implicit val executionContext: ExecutionContextExecutor =
        actorSystem.dispatcher
      implicit val system: ActorSystem = actorSystem
      implicit val materializer: Materializer =
        ActorMaterializer()(actorSystem)

      val flow = ElasticsearchHitToDisplayWorkFlow()

      val work = IdentifiedWork(
        canonicalId = "t83tggem",
        title = Some("Tired of troubling tests"),
        sourceIdentifier = SourceIdentifier(
          identifierScheme = IdentifierSchemes.miroImageNumber,
          ontologyType = "work",
          value = "T0083000"
        ),
        version = 1
      )

      val elasticsearchHitJson = s"""{
        "_index": "zagbdjgf",
        "_type": "work",
        "_id": "${work.canonicalId}",
        "_score": 1,
        "_source": ${toJson(work).get}
      }"""

      val futureDisplayWork: Future[DisplayWork] = Source
        .single(elasticsearchHitJson)
        .via(flow)
        .runWith(Sink.head)

      whenReady(futureDisplayWork) { displayWork =>
        displayWork shouldBe DisplayWork(work, includes = includes)
      }
    }
  }
  //
  // it("creates a SierraRecord from an item") {
  //   withRecordWrapperFlow {
  //     case (wrapperFlow, actorSystem) =>
  //       implicit val system: ActorSystem = actorSystem
  //       implicit val materializer: Materializer =
  //         ActorMaterializer()(actorSystem)
  //
  //       val id = "400004"
  //       val updatedDate = "2014-04-14T14:14:14Z"
  //       val json = parse(s"""
  //       |{
  //       | "id": "$id",
  //       | "updatedDate": "$updatedDate",
  //       | "bibIds": ["4", "44", "444", "4444"]
  //       |}
  //     """.stripMargin).right.get
  //
  //       val expectedRecord = SierraRecord(
  //         id = id,
  //         data = json.noSpaces,
  //         modifiedDate = updatedDate
  //       )
  //
  //       val futureRecord = Source
  //         .single(json)
  //         .via(wrapperFlow)
  //         .runWith(Sink.head)
  //
  //       whenReady(futureRecord) { sierraRecord =>
  //         sierraRecord shouldBe expectedRecord
  //       }
  //   }
  // }
  //
  // it("is able to handle deleted bibs") {
  //   withRecordWrapperFlow {
  //     case (wrapperFlow, actorSystem) =>
  //       implicit val system: ActorSystem = actorSystem
  //       implicit val materializer: Materializer =
  //         ActorMaterializer()(actorSystem)
  //
  //       val id = "1357947"
  //       val deletedDate = "2014-01-31"
  //       val json = parse(s"""{
  //                         |  "id" : "$id",
  //                         |  "deletedDate" : "$deletedDate",
  //                         |  "deleted" : true
  //                         |}""".stripMargin).right.get
  //
  //       val expectedRecord = SierraRecord(
  //         id = id,
  //         data = json.noSpaces,
  //         modifiedDate = s"${deletedDate}T00:00:00Z"
  //       )
  //
  //       val futureRecord = Source
  //         .single(json)
  //         .via(wrapperFlow)
  //         .runWith(Sink.head)
  //
  //       whenReady(futureRecord) { sierraRecord =>
  //         sierraRecord shouldBe expectedRecord
  //       }
  //   }
  // }
  //
  // it("fails the stream if the record contains invalid JSON") {
  //   withRecordWrapperFlow {
  //     case (wrapperFlow, actorSystem) =>
  //       implicit val system: ActorSystem = actorSystem
  //       implicit val materializer: Materializer =
  //         ActorMaterializer()(actorSystem)
  //
  //       val invalidSierraJson = parse(s"""{
  //       | "missing": ["id", "updatedDate"],
  //       | "reason": "This JSON will not pass!",
  //       |  "comment": "XML is coming!"
  //       |}""".stripMargin).right.get
  //
  //       val futureUnit = Source
  //         .single(invalidSierraJson)
  //         .via(wrapperFlow)
  //         .runWith(Sink.head)
  //
  //       whenReady(futureUnit.failed) { _ =>
  //         true shouldBe true
  //       }
  //   }
  // }
}
