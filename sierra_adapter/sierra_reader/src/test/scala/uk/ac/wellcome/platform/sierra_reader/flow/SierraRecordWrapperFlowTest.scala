package uk.ac.wellcome.platform.sierra_reader.flow

import java.time.Instant

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import io.circe.Json
import io.circe.parser._
import org.scalatest.compatible.Assertion
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.{
  AbstractSierraRecord,
  SierraBibRecord,
  SierraItemRecord
}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions

class SierraRecordWrapperFlowTest
    extends FunSpec
    with Akka
    with ScalaFutures
    with IntegrationPatience
    with Matchers
    with JsonAssertions
    with SierraGenerators {

  private def withRecordWrapperFlow[T <: AbstractSierraRecord](
    createRecord: (String, String, Instant) => T)(
    testWith: TestWith[Flow[Json, T, NotUsed], Assertion]) = {
    val wrapperFlow = SierraRecordWrapperFlow(
      createRecord = createRecord
    )

    testWith(wrapperFlow)
  }

  it("creates a SierraRecord from a bib") {
    withMaterializer { implicit materializer =>
      withRecordWrapperFlow(SierraBibRecord.apply) { wrapperFlow =>
        val id = createSierraBibNumber
        val updatedDate = "2013-12-13T12:43:16Z"
        val jsonString =
          s"""
          |{
          |  "id": "$id",
          |  "updatedDate": "$updatedDate"
          |}
          """.stripMargin
        val expectedRecord = createSierraBibRecordWith(
          id = id,
          data = jsonString,
          modifiedDate = Instant.parse(updatedDate)
        )

        val json = parse(jsonString).right.get

        val futureRecord = Source
          .single(json)
          .via(wrapperFlow)
          .runWith(Sink.head)
        whenReady(futureRecord) { sierraRecord =>
          assertSierraRecordsAreEqual(sierraRecord, expectedRecord)
        }
      }
    }
  }

  it("creates a SierraRecord from an item") {
    withMaterializer { implicit materializer =>
      withRecordWrapperFlow(SierraItemRecord.apply) { wrapperFlow =>
        val id = createSierraItemNumber
        val updatedDate = "2014-04-14T14:14:14Z"

        // We need to encode the bib IDs as strings, _not_ as typed
        // objects -- the format needs to map what we get from the
        // Sierra API.
        //
        val bibIds = (1 to 4).map { _ =>
          createSierraRecordNumberString
        }
        val jsonString =
          s"""
          |{
          | "id": "$id",
          | "updatedDate": "$updatedDate",
          | "bibIds": ${toJson(bibIds).get}
          |}
          """.stripMargin

        val expectedRecord = createSierraItemRecordWith(
          id = id,
          data = jsonString,
          modifiedDate = Instant.parse(updatedDate)
        )

        val json = parse(jsonString).right.get

        val futureRecord = Source
          .single(json)
          .via(wrapperFlow)
          .runWith(Sink.head)

        whenReady(futureRecord) { sierraRecord =>
          assertSierraRecordsAreEqual(sierraRecord, expectedRecord)
        }
      }
    }
  }

  it("is able to handle deleted bibs") {
    withMaterializer { implicit materializer =>
      withRecordWrapperFlow(SierraBibRecord.apply) { wrapperFlow =>
        val id = createSierraBibNumber
        val deletedDate = "2014-01-31"
        val jsonString =
          s"""
          |{
          |  "id" : "$id",
          |  "deletedDate" : "$deletedDate",
          |  "deleted" : true
          |}
          |""".stripMargin

        val expectedRecord = createSierraBibRecordWith(
          id = id,
          data = jsonString,
          modifiedDate = Instant.parse(s"${deletedDate}T00:00:00Z")
        )

        val json = parse(jsonString).right.get

        val futureRecord = Source
          .single(json)
          .via(wrapperFlow)
          .runWith(Sink.head)
        whenReady(futureRecord) { sierraRecord =>
          assertSierraRecordsAreEqual(sierraRecord, expectedRecord)
        }
      }
    }
  }

  it("fails the stream if the record contains invalid JSON") {
    withMaterializer { implicit materializer =>
      withRecordWrapperFlow(SierraBibRecord.apply) { wrapperFlow =>
        val invalidSierraJson = parse(s"""
          |{
          |  "missing": ["id", "updatedDate"],
          |  "reason": "This JSON will not pass!",
          |  "comment": "XML is coming!"
          |}
          |""".stripMargin).right.get

        val futureUnit = Source
          .single(invalidSierraJson)
          .via(wrapperFlow)
          .runWith(Sink.head)

        whenReady(futureUnit.failed) { _ =>
          true shouldBe true
        }
      }
    }
  }

  private def assertSierraRecordsAreEqual(
    x: AbstractSierraRecord,
    y: AbstractSierraRecord): Assertion = {
    x.id shouldBe x.id
    assertJsonStringsAreEqual(x.data, y.data)
    x.modifiedDate shouldBe y.modifiedDate
  }
}
