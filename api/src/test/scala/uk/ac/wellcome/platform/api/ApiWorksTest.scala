package uk.ac.wellcome.platform.api

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import com.sksamuel.elastic4s.ElasticDsl._
import uk.ac.wellcome.models.{
  IdentifiedUnifiedItem,
  SourceIdentifier,
  UnifiedItem
}
import uk.ac.wellcome.platform.api.models.Record
import uk.ac.wellcome.platform.api.responses.ResultListResponse
import uk.ac.wellcome.test.utils.ElasticSearchLocal
import uk.ac.wellcome.utils.JsonUtil

class ApiWorksTest extends FeatureTest with ElasticSearchLocal {
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
    implicit val jsonMapper = IdentifiedUnifiedItem
    val identifiedUnifiedItem = IdentifiedUnifiedItem(
      canonicalId = "1234",
      unifiedItem =
        UnifiedItem(identifiers =
                      List(SourceIdentifier("Miro", "MiroID", "5678")),
                    label = "this is the image title"))
    elasticClient.execute(
      indexInto("records" / "item").doc(identifiedUnifiedItem))

    eventually {
      server.httpGet(
        path = "/catalogue/v0/works",
        andExpect = Status.Ok,
        withJsonBody = JsonUtil
          .toJson(ResultListResponse(
            context =
              s"http://${server.externalHttpHostAndPort}/catalogue/v0/context.json",
            ontologyType = "ResultList",
            pageSize = 10,
            results = Array(Record(ontologyType = "Work",
                                   id = "1234",
                                   label = "this is the image title"))
          ))
          .get
      )
    }
  }
}
