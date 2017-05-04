package uk.ac.wellcome.platform.api

import com.sksamuel.elastic4s.ElasticDsl._
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.models.{
  IdentifiedUnifiedItem,
  SourceIdentifier,
  UnifiedItem
}
import uk.ac.wellcome.test.utils.ElasticSearchLocal

class ApiWorksTest extends FeatureTest with ElasticSearchLocal {

  implicit val jsonMapper = IdentifiedUnifiedItem
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
  val itemType = "item"

  test("it should return a list of works") {

    val firstIdentifiedUnifiedItem =
      identifiedUnifiedItemWith(canonicalId = "1234",
                                label = "this is the first image label")
    val secondIdentifiedUnifiedItem =
      identifiedUnifiedItemWith(canonicalId = "4321",
                                label = "this is the second image label")
    val thirdIdentifiedUnifiedItem =
      identifiedUnifiedItemWith(canonicalId = "9876",
                                label = "this is the third image label")

    insertIntoElasticSearch(firstIdentifiedUnifiedItem)
    insertIntoElasticSearch(secondIdentifiedUnifiedItem)
    insertIntoElasticSearch(thirdIdentifiedUnifiedItem)

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
            |     "id": "${firstIdentifiedUnifiedItem.canonicalId}",
            |     "label": "${firstIdentifiedUnifiedItem.unifiedItem.label}"
            |   },
            |   {
            |     "type": "Work",
            |     "id": "${secondIdentifiedUnifiedItem.canonicalId}",
            |     "label": "${secondIdentifiedUnifiedItem.unifiedItem.label}"
            |   },
            |   {
            |     "type": "Work",
            |     "id": "${thirdIdentifiedUnifiedItem.canonicalId}",
            |     "label": "${thirdIdentifiedUnifiedItem.unifiedItem.label}"
            |   }
            |  ]
            |}
          """.stripMargin
      )
    }
  }

  test("it should return a single work when requested with id") {
    val identifiedUnifiedItem =
      identifiedUnifiedItemWith(canonicalId = "1234",
                                label = "this is the first image title")
    insertIntoElasticSearch(identifiedUnifiedItem)

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

  private def insertIntoElasticSearch(
    identifiedUnifiedItem: IdentifiedUnifiedItem): Any = {
    elasticClient.execute(
      indexInto(index / itemType).doc(identifiedUnifiedItem))
  }

  private def identifiedUnifiedItemWith(canonicalId: String, label: String) = {
    IdentifiedUnifiedItem(canonicalId = canonicalId,
                          unifiedItem = UnifiedItem(
                            identifiers =
                              List(SourceIdentifier("Miro", "MiroID", "5678")),
                            label = label))
  }
}
