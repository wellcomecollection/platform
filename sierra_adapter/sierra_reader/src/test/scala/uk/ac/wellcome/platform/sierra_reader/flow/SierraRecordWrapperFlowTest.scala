package uk.ac.wellcome.platform.sierra_reader.flow

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import io.circe.Json
import io.circe.parser._
import org.scalatest.compatible.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.sierra_adapter.models.SierraRecord
import uk.ac.wellcome.sierra_adapter.test.utils.SierraRecordUtil
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.{ExtendedPatience, JsonTestUtil}
import uk.ac.wellcome.utils.JsonUtil._

class SierraRecordWrapperFlowTest
    extends FunSpec
    with Akka
    with ScalaFutures
    with ExtendedPatience
    with Matchers
    with JsonTestUtil
    with SierraRecordUtil {

  private def withRecordWrapperFlow(actorSystem: ActorSystem)(
    testWith: TestWith[Flow[Json, SierraRecord, NotUsed], Assertion]) = {
    val wrapperFlow = SierraRecordWrapperFlow()

    testWith(wrapperFlow)
  }

  it("creates a SierraRecord from a bib") {
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
        withRecordWrapperFlow(actorSystem) { wrapperFlow =>
          val id = createSierraRecordNumberString
          val updatedDate = "2013-12-13T12:43:16Z"
          val jsonString = s"""
               |{
               | "id": "$id",
               | "updatedDate": "$updatedDate"
               |}
            """.stripMargin

          val expectedRecord = createSierraRecordWith(
            id = id,
            data = jsonString,
            modifiedDate = updatedDate
          )

          val json = parse(jsonString).right.get

          val futureRecord = Source
            .single(json)
            .via(wrapperFlow)
            .runWith(Sink.head)(materializer)

          whenReady(futureRecord) { sierraRecord =>
            assertSierraRecordsAreEqual(sierraRecord, expectedRecord)
          }
        }
      }
    }
  }

  it("creates a SierraRecord from an item") {
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
        withRecordWrapperFlow(actorSystem) { wrapperFlow =>
          val id = createSierraRecordNumberString
          val updatedDate = "2014-04-14T14:14:14Z"
          val jsonString = s"""
          |{
          | "id": "$id",
          | "updatedDate": "$updatedDate",
          | "bibIds": ${toJson(createSierraRecordNumberStrings(count = 4)).get}
          |}
        """.stripMargin

          val expectedRecord = createSierraRecordWith(
            id = id,
            data = jsonString,
            modifiedDate = updatedDate
          )

          val json = parse(jsonString).right.get

          val futureRecord = Source
            .single(json)
            .via(wrapperFlow)
            .runWith(Sink.head)(materializer)

          whenReady(futureRecord) { sierraRecord =>
            assertSierraRecordsAreEqual(sierraRecord, expectedRecord)
          }
        }
      }
    }
  }

  it("is able to handle deleted bibs") {
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
        withRecordWrapperFlow(actorSystem) { wrapperFlow =>
          val id = createSierraRecordNumberString
          val deletedDate = "2014-01-31"
          val jsonString = s"""{
            |  "id" : "$id",
            |  "deletedDate" : "$deletedDate",
            |  "deleted" : true
            |}""".stripMargin

          val expectedRecord = createSierraRecordWith(
            id = id,
            data = jsonString,
            modifiedDate = s"${deletedDate}T00:00:00Z"
          )

          val json = parse(jsonString).right.get

          val futureRecord = Source
            .single(json)
            .via(wrapperFlow)
            .runWith(Sink.head)(materializer)

          whenReady(futureRecord) { sierraRecord =>
            assertSierraRecordsAreEqual(sierraRecord, expectedRecord)
          }
        }
      }
    }
  }

  it("fails the stream if the record contains invalid JSON") {
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
        withRecordWrapperFlow(actorSystem) { wrapperFlow =>
          val invalidSierraJson = parse(s"""{
          | "missing": ["id", "updatedDate"],
          | "reason": "This JSON will not pass!",
          |  "comment": "XML is coming!"
          |}""".stripMargin).right.get

          val futureUnit = Source
            .single(invalidSierraJson)
            .via(wrapperFlow)
            .runWith(Sink.head)(materializer)

          whenReady(futureUnit.failed) { _ =>
            true shouldBe true
          }
        }
      }
    }
  }

  private def assertSierraRecordsAreEqual(x: SierraRecord,
                                          y: SierraRecord): Assertion = {
    x.id shouldBe x.id
    assertJsonStringsAreEqual(x.data, y.data)
    x.modifiedDate shouldBe y.modifiedDate
  }
}
