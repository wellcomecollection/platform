package uk.ac.wellcome.platform.api

import com.sksamuel.elastic4s.ElasticDsl._
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.models.{IdentifiedWork, SourceIdentifier, Work}
import uk.ac.wellcome.test.utils.IndexedElasticSearchLocal

class ApiWorksTest extends FeatureTest with IndexedElasticSearchLocal {

  implicit val jsonMapper = IdentifiedWork
  override val server =
    new EmbeddedHttpServer(
      new Server,
      flags = Map(
        "es.host" -> "localhost",
        "es.port" -> 9300.toString,
        "es.name" -> "wellcome",
        "es.xpack.enabled" -> "true",
        "es.xpack.user" -> "elastic:changeme",
        "es.xpack.sslEnabled" -> "false",
        "es.sniff" -> "false"
      )
    )

  test("it should return a list of works") {

    val firstIdentifiedWork =
      identifiedWorkWith(canonicalId = "1234",
                         label = "this is the first image label")
    val secondIdentifiedWork =
      identifiedWorkWith(canonicalId = "4321",
                         label = "this is the second image label")
    val thirdIdentifiedWork =
      identifiedWorkWith(canonicalId = "9876",
                         label = "this is the third image label")

    insertIntoElasticSearch(firstIdentifiedWork)
    insertIntoElasticSearch(secondIdentifiedWork)
    insertIntoElasticSearch(thirdIdentifiedWork)

    eventually {
      server.httpGet(
        path = "/catalogue/v0/works",
        andExpect = Status.Ok,
        withJsonBody = s"""
            |{
            |  "@context": "http://localhost:8888/catalogue/v0/context.json",
            |  "type": "ResultList",
            |  "pageSize": 10,
            |  "results": [
            |   {
            |     "type": "Work",
            |     "id": "${firstIdentifiedWork.canonicalId}",
            |     "label": "${firstIdentifiedWork.work.label}"
            |   },
            |   {
            |     "type": "Work",
            |     "id": "${secondIdentifiedWork.canonicalId}",
            |     "label": "${secondIdentifiedWork.work.label}"
            |   },
            |   {
            |     "type": "Work",
            |     "id": "${thirdIdentifiedWork.canonicalId}",
            |     "label": "${thirdIdentifiedWork.work.label}"
            |   }
            |  ]
            |}
          """.stripMargin
      )
    }
  }

  test("it should return a single work when requested with id") {
    val identifiedWork =
      identifiedWorkWith(canonicalId = "1234",
                         label = "this is the first image title")
    insertIntoElasticSearch(identifiedWork)

    eventually {
      server.httpGet(
        path = "/catalogue/v0/works/1234",
        andExpect = Status.Ok,
        withJsonBody = s"""
            |{
            | "@context": "http://localhost:8888/catalogue/v0/context.json",
            | "type": "Work",
            | "id": "1234",
            | "label": "this is the first image title"
            |}
          """.stripMargin
      )
    }
  }

  test("it should return a not found error when requesting a single work with a non existing id") {
    server.httpGet(
      path = "/catalogue/v0/works/non-existing-id",
      andExpect = Status.NotFound,
      withJsonBody = ""
    )
  }

  private def insertIntoElasticSearch(identifiedWork: IdentifiedWork): Any = {
    elasticClient.execute(indexInto(indexName / itemType).doc(identifiedWork))
  }

  private def identifiedWorkWith(canonicalId: String, label: String) = {
    IdentifiedWork(canonicalId = canonicalId,
                   work =
                     Work(identifiers =
                            List(SourceIdentifier("Miro", "MiroID", "5678")),
                          label = label))
  }
}
