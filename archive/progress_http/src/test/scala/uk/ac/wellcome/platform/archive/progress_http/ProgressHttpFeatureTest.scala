package uk.ac.wellcome.platform.archive.progress_http

import java.time.Instant
import java.util.UUID

import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import io.circe.optics.JsonPath.root
import io.circe.parser._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressTrackerFixture
import uk.ac.wellcome.platform.archive.common.progress.models._
import uk.ac.wellcome.platform.archive.display._
import uk.ac.wellcome.platform.archive.progress_http.fixtures.ProgressHttpFixture
import uk.ac.wellcome.platform.archive.progress_http.model.ErrorResponse
import uk.ac.wellcome.storage.ObjectLocation

class ProgressHttpFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with ProgressTrackerFixture
    with ProgressHttpFixture
    with RandomThings
    with Inside
    with IntegrationPatience
    with JsonAssertions {

  import HttpMethods._
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import uk.ac.wellcome.json.JsonUtil._
  import uk.ac.wellcome.storage.dynamo._
  import Progress._

  val contextUrl = "http://api.wellcomecollection.org/storage/v1/context.json"
  describe("GET /progress/:id") {
    it("returns a progress tracker when available") {
      withConfiguredApp {
        case (table, _, baseUrl) =>
          withMaterializer { implicit materialiser =>
            withProgressTracker(table) { progressTracker =>
              val progress = createProgress

              val expectedSourceLocationJson =
                s"""{
                  "provider": {
                    "id": "${StandardDisplayProvider.id}",
                    "type": "Provider"
                  },
                  "bucket": "${progress.sourceLocation.location.namespace}",
                  "path": "${progress.sourceLocation.location.key}",
                  "type": "Location"
                }""".stripMargin

              val expectedCallbackJson =
                s"""{
                  "url": "${progress.callback.get.uri}",
                  "status": {
                    "id": "processing",
                    "type": "Status"
                  },
                  "type": "Callback"
                }""".stripMargin

              val expectedIngestTypeJson = s"""{
                "id": "create",
                "type": "IngestType"
              }""".stripMargin

              val expectedSpaceJson = s"""{
                "id": "${progress.space.underlying}",
                "type": "Space"
              }""".stripMargin

              val expectedStatusJson = s"""{
                "id": "accepted",
                "type": "Status"
              }""".stripMargin

              whenReady(progressTracker.initialise(progress)) { _ =>
                whenGetRequestReady(s"$baseUrl/progress/${progress.id}") {
                  result =>
                    result.status shouldBe StatusCodes.OK

                    withStringEntity(result.entity) { jsonString =>
                      val json = parse(jsonString).right.get
                      root.`@context`.string
                        .getOption(json)
                        .get shouldBe "http://api.wellcomecollection.org/storage/v1/context.json"
                      root.id.string
                        .getOption(json)
                        .get shouldBe progress.id.toString

                      assertJsonStringsAreEqual(
                        root.sourceLocation.json.getOption(json).get.noSpaces,
                        expectedSourceLocationJson)

                      assertJsonStringsAreEqual(
                        root.callback.json.getOption(json).get.noSpaces,
                        expectedCallbackJson)
                      assertJsonStringsAreEqual(
                        root.ingestType.json.getOption(json).get.noSpaces,
                        expectedIngestTypeJson)
                      assertJsonStringsAreEqual(
                        root.space.json.getOption(json).get.noSpaces,
                        expectedSpaceJson)
                      assertJsonStringsAreEqual(
                        root.status.json.getOption(json).get.noSpaces,
                        expectedStatusJson)
                      assertJsonStringsAreEqual(
                        root.events.json.getOption(json).get.noSpaces,
                        "[]")

                      root.`type`.string.getOption(json).get shouldBe "Ingest"

                      assertRecent(Instant.parse(
                        root.createdDate.string.getOption(json).get))
                      assertRecent(Instant.parse(
                        root.lastModifiedDate.string.getOption(json).get))
                    }
                }
              }
            }
          }
      }
    }

    it("does not output empty values") {
      withConfiguredApp {
        case (table, _, baseUrl) =>
          withMaterializer { implicit materialiser =>
            withProgressTracker(table) { progressTracker =>
              val progress = createProgressWith(callback = None)
              whenReady(progressTracker.initialise(progress)) { _ =>
                whenGetRequestReady(s"$baseUrl/progress/${progress.id}") {
                  result =>
                    result.status shouldBe StatusCodes.OK
                    withStringEntity(result.entity) { jsonString =>
                      val infoJson = parse(jsonString).right.get
                      infoJson.findAllByKey("callback") shouldBe empty
                    }
                }
              }
            }
          }
      }
    }

    it("returns a 404 NotFound if no progress tracker matches id") {
      withConfiguredApp {
        case (_, _, baseUrl) =>
          whenGetRequestReady(s"$baseUrl/progress/$randomUUID") { response =>
            response.status shouldBe StatusCodes.NotFound
            response.entity.contentType shouldBe ContentTypes.`application/json`
          }
      }
    }
  }

  describe("POST /progress") {
    it("creates a progress tracker") {
      withConfiguredApp {
        case (table, topic, baseUrl) =>
          withMaterializer { implicit materialiser =>
            val url = s"$baseUrl/progress"

            val bucketName = "bucket"
            val s3key = "key.txt"
            val spaceName = "somespace"
            val entity = HttpEntity(
              ContentTypes.`application/json`,
              s"""|{
                 |  "type": "Ingest",
                 |  "ingestType": {
                 |    "id": "create",
                 |    "type": "IngestType"
                 |  },
                 |  "sourceLocation":{
                 |    "type": "Location",
                 |    "provider": {
                 |      "type": "Provider",
                 |      "id": "${StandardDisplayProvider.id}"
                 |    },
                 |    "bucket": "$bucketName",
                 |    "path": "$s3key"
                 |  },
                 |  "space": {
                 |    "id": "$spaceName",
                 |    "type": "Space"
                 |  },
                 |  "callback": {
                 |    "url": "${testCallbackUri.toString}"
                 |  }
                 |}""".stripMargin
            )

            val request = HttpRequest(
              method = POST,
              uri = url,
              headers = Nil,
              entity = entity
            )

            val expectedLocationR = s"$baseUrl/(.+)".r

            whenRequestReady(request) { response: HttpResponse =>
              response.status shouldBe StatusCodes.Created

              val maybeId = response.headers.collectFirst {
                case HttpHeader("location", expectedLocationR(id)) => id
              }

              maybeId.isEmpty shouldBe false
              val id = UUID.fromString(maybeId.get)

              val progressFuture =
                Unmarshal(response.entity).to[ResponseDisplayIngest]

              whenReady(progressFuture) { actualProgress =>
                inside(actualProgress) {
                  case ResponseDisplayIngest(
                      actualContextUrl,
                      actualId,
                      actualSourceLocation,
                      Some(
                        DisplayCallback(
                          actualCallbackUrl,
                          Some(DisplayStatus(actualCallbackStatus, "Status")),
                          "Callback")),
                      CreateDisplayIngestType,
                      DisplayStorageSpace(actualSpaceId, "Space"),
                      DisplayStatus("accepted", "Status"),
                      None,
                      Nil,
                      actualCreatedDate,
                      actualLastModifiedDate,
                      "Ingest") =>
                    actualContextUrl shouldBe contextUrl
                    actualId shouldBe id
                    actualSourceLocation shouldBe DisplayLocation(
                      StandardDisplayProvider,
                      bucketName,
                      s3key)
                    actualCallbackUrl shouldBe testCallbackUri.toString
                    actualCallbackStatus shouldBe "processing"
                    actualSpaceId shouldBe spaceName

                    assertTableOnlyHasItem[Progress](
                      Progress(
                        id,
                        StorageLocation(
                          StandardStorageProvider,
                          ObjectLocation(bucketName, s3key)),
                        Namespace(spaceName),
                        Some(Callback(testCallbackUri, Callback.Pending)),
                        Progress.Accepted,
                        None,
                        Instant.parse(actualCreatedDate),
                        Instant.parse(actualLastModifiedDate),
                        Nil
                      ),
                      table
                    )
                }

                val requests =
                  listMessagesReceivedFromSNS(topic).map(messageInfo =>
                    fromJson[IngestBagRequest](messageInfo.message).get)

                requests shouldBe List(
                  IngestBagRequest(
                    id,
                    storageSpace = StorageSpace(spaceName),
                    archiveCompleteCallbackUrl = Some(testCallbackUri),
                    zippedBagLocation = ObjectLocation("bucket", "key.txt")
                  ))
              }
            }
          }
      }
    }

    it(
      "returns a json error if the ingest request doesn't have a sourceLocation") {
      withConfiguredApp {
        case (_, topic, baseUrl) =>
          withMaterializer { implicit materialiser =>
            val url = s"$baseUrl/progress"

            val entity = HttpEntity(
              ContentTypes.`application/json`,
              """|{
                 |  "type": "Ingest",
                 |  "ingestType": {
                 |    "id": "create",
                 |    "type": "IngestType"
                 |  },
                 |  "space": {
                 |    "id": "bcnfgh",
                 |    "type": "Space"
                 |  }
                 |}""".stripMargin
            )

            val request = HttpRequest(
              method = POST,
              uri = url,
              headers = Nil,
              entity = entity
            )

            whenRequestReady(request) { response: HttpResponse =>

              response.status shouldBe StatusCodes.BadRequest
              response.entity.contentType shouldBe ContentTypes.`application/json`

              val progressFuture =
                Unmarshal(response.entity).to[ErrorResponse]

              whenReady(progressFuture) { actualError =>
                actualError shouldBe ErrorResponse(
                  400,
                  "Invalid value at .sourceLocation: required property not supplied.",
                  "Bad Request",
                  "Error")
                assertSnsReceivesNothing(topic)
              }
            }
          }
      }
    }

    it(
      "returns a json error if the ingest request doesn't have a sourceLocation and it doesn't have an ingestType") {
      withConfiguredApp {
        case (_, topic, baseUrl) =>
          withMaterializer { implicit materialiser =>
            val url = s"$baseUrl/progress"

            val entity = HttpEntity(
              ContentTypes.`application/json`,
              """|{
                 |  "type": "Ingest",
                 |  "space": {
                 |    "id": "bcnfgh",
                 |    "type": "Space"
                 |  }
                 |}""".stripMargin
            )

            val request = HttpRequest(
              method = POST,
              uri = url,
              headers = Nil,
              entity = entity
            )

            whenRequestReady(request) { response: HttpResponse =>
              response.status shouldBe StatusCodes.BadRequest
              response.entity.contentType shouldBe ContentTypes.`application/json`

              val progressFuture =
                Unmarshal(response.entity).to[ErrorResponse]

              whenReady(progressFuture) { actualError =>
                actualError shouldBe ErrorResponse(
                  400,
                  """|Invalid value at .sourceLocation: required property not supplied.
                     |Invalid value at .ingestType: required property not supplied.""".stripMargin,
                  "Bad Request",
                  "Error")
                assertSnsReceivesNothing(topic)
              }
            }
          }
      }
    }

    it("returns a json error if the sourceLocation doesn't have a bucket field") {
      withConfiguredApp {
        case (_, topic, baseUrl) =>
          withMaterializer { implicit materialiser =>
            val url = s"$baseUrl/progress"

            val entity = HttpEntity(
              ContentTypes.`application/json`,
              s"""|{
                 |  "type": "Ingest",
                 |  "ingestType": {
                 |    "id": "create",
                 |    "type": "IngestType"
                 |  },
                 |  "sourceLocation":{
                 |    "type": "Location",
                 |    "provider": {
                 |      "type": "Provider",
                 |      "id": "${StandardDisplayProvider.id}"
                 |    },
                 |    "path": "b22454408.zip"
                 |  },
                 |  "space": {
                 |    "id": "bcnfgh",
                 |    "type": "Space"
                 |  }
                 |}""".stripMargin
            )

            val request = HttpRequest(
              method = POST,
              uri = url,
              headers = Nil,
              entity = entity
            )

            whenRequestReady(request) { response: HttpResponse =>
              response.status shouldBe StatusCodes.BadRequest
              response.entity.contentType shouldBe ContentTypes.`application/json`

              val progressFuture =
                Unmarshal(response.entity).to[ErrorResponse]

              whenReady(progressFuture) { actualError =>
                actualError shouldBe ErrorResponse(
                  400,
                  "Invalid value at .sourceLocation.bucket: required property not supplied.",
                  "Bad Request",
                  "Error")
                assertSnsReceivesNothing(topic)
              }
            }
          }
      }
    }

    it("returns a json error if the sourceLocation has an invalid bucket field") {
      withConfiguredApp {
        case (_, topic, baseUrl) =>
          withMaterializer { implicit materialiser =>
            val url = s"$baseUrl/progress"

            val entity = HttpEntity(
              ContentTypes.`application/json`,
              s"""|{
                  |  "type": "Ingest",
                  |  "ingestType": {
                  |    "id": "create",
                  |    "type": "IngestType"
                  |  },
                  |  "sourceLocation":{
                  |    "type": "Location",
                  |    "provider": {
                  |      "type": "Provider",
                  |      "id": "${StandardDisplayProvider.id}"
                  |    },
                  |    "bucket": {"name": "bucket"},
                  |    "path": "b22454408.zip"
                  |  },
                  |  "space": {
                  |    "id": "bcnfgh",
                  |    "type": "Space"
                  |  }
                  |}""".stripMargin
            )

            val request = HttpRequest(
              method = POST,
              uri = url,
              headers = Nil,
              entity = entity
            )

            whenRequestReady(request) { response: HttpResponse =>
              response.status shouldBe StatusCodes.BadRequest
              response.entity.contentType shouldBe ContentTypes.`application/json`

              val progressFuture =
                Unmarshal(response.entity).to[ErrorResponse]

              whenReady(progressFuture) { actualError =>
                actualError shouldBe ErrorResponse(
                  400,
                  "Invalid value at .sourceLocation.bucket: should be a String.",
                  "Bad Request",
                  "Error")
                assertSnsReceivesNothing(topic)
              }
            }
          }
      }
    }

    it("returns a json error if the provider doesn't have a valid id field") {
      withConfiguredApp {
        case (_, topic, baseUrl) =>
          withMaterializer { implicit materialiser =>
            val url = s"$baseUrl/progress"

            val entity = HttpEntity(
              ContentTypes.`application/json`,
              """|{
                 |  "type": "Ingest",
                 |  "ingestType": {
                 |    "id": "create",
                 |    "type": "IngestType"
                 |  },
                 |  "sourceLocation":{
                 |    "type": "Location",
                 |    "provider": {
                 |      "type": "Provider",
                 |      "id": "blipbloop"
                 |    },
                 |    "bucket": "bucket",
                 |    "path": "b22454408.zip"
                 |  },
                 |  "space": {
                 |    "id": "bcnfgh",
                 |    "type": "Space"
                 |  }
                 |}""".stripMargin
            )

            val request = HttpRequest(
              method = POST,
              uri = url,
              headers = Nil,
              entity = entity
            )

            whenRequestReady(request) { response: HttpResponse =>
              response.status shouldBe StatusCodes.BadRequest
              response.entity.contentType shouldBe ContentTypes.`application/json`

              val progressFuture =
                Unmarshal(response.entity).to[ErrorResponse]

              whenReady(progressFuture) { actualError =>
                actualError shouldBe ErrorResponse(
                  400,
                  """Invalid value at .sourceLocation.provider.id: got "blipbloop", valid values are: aws-s3-standard, aws-s3-ia.""",
                  "Bad Request",
                  "Error")
                assertSnsReceivesNothing(topic)
              }
            }
          }
      }
    }

    it("returns a json error if the ingestType doesn't have a valid id field") {
      withConfiguredApp {
        case (_, topic, baseUrl) =>
          withMaterializer { implicit materialiser =>
            val url = s"$baseUrl/progress"

            val entity = HttpEntity(
              ContentTypes.`application/json`,
              """|{
                 |  "type": "Ingest",
                 |  "ingestType": {
                 |    "id": "baboop",
                 |    "type": "IngestType"
                 |  },
                 |  "sourceLocation":{
                 |    "type": "Location",
                 |    "provider": {
                 |      "type": "Provider",
                 |      "id": "aws-s3-standard"
                 |    },
                 |    "bucket": "bucket",
                 |    "path": "b22454408.zip"
                 |  },
                 |  "space": {
                 |    "id": "bcnfgh",
                 |    "type": "Space"
                 |  }
                 |}""".stripMargin
            )

            val request = HttpRequest(
              method = POST,
              uri = url,
              headers = Nil,
              entity = entity
            )

            whenRequestReady(request) { response: HttpResponse =>
              response.status shouldBe StatusCodes.BadRequest
              response.entity.contentType shouldBe ContentTypes.`application/json`

              val progressFuture =
                Unmarshal(response.entity).to[ErrorResponse]

              whenReady(progressFuture) { actualError =>

                actualError shouldBe ErrorResponse(400, """Invalid value at .ingestType.id: got "baboop", valid values are: create.""", "Bad Request", "Error")

                assertSnsReceivesNothing(topic)
              }
            }
          }
      }
    }
  }
}
