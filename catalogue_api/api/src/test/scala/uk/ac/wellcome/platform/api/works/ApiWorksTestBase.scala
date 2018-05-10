package uk.ac.wellcome.platform.api.works

import com.sksamuel.elastic4s.Indexable
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.DisplaySerialisationTestBase
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.work.internal.IdentifiedWork
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.platform.api.Server
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.versions.ApiVersions

trait ApiWorksTestBase
    extends FunSpec
    with ElasticsearchFixtures
    with DisplaySerialisationTestBase
    with WorksUtil {

  implicit object IdentifiedWorkIndexable extends Indexable[IdentifiedWork] {
    override def json(t: IdentifiedWork): String =
      toJson(t).get
  }

  def withServer[R](
    indexNameV1: String,
    indexNameV2: String,
    itemType: String = "work")(testWith: TestWith[EmbeddedHttpServer, R]) = {

    val server: EmbeddedHttpServer = new EmbeddedHttpServer(
      new Server,
      flags = Map(
        "es.host" -> "localhost",
        "es.port" -> 9200.toString,
        "es.index.v1" -> indexNameV1,
        "es.index.v2" -> indexNameV2,
        "es.type" -> itemType
      )
    )

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }

  def withApiFixtures[R](apiVersion: ApiVersions.Value,
                         apiName: String = "catalogue/",
                         itemType: String = "work")(
    testWith: TestWith[(String, String, String, String, EmbeddedHttpServer),
                       R]) =
    withLocalElasticsearchIndex(itemType = itemType) { indexV1 =>
      withLocalElasticsearchIndex(itemType = itemType) { indexV2 =>
        withServer(indexV1, indexV2, itemType) { server =>
          testWith((apiName + apiVersion, indexV1, indexV2, itemType, server))
        }
      }
    }

  def emptyJsonResult(apiPrefix: String): String = s"""
    |{
    |  "@context": "https://localhost:8888/$apiPrefix/context.json",
    |  "type": "ResultList",
    |  "pageSize": 10,
    |  "totalPages": 0,
    |  "totalResults": 0,
    |  "results": []
    |}""".stripMargin

  def badRequest(apiPrefix: String, description: String) =
    s"""{
      "@context": "https://localhost:8888/$apiPrefix/context.json",
      "type": "Error",
      "errorType": "http",
      "httpStatus": 400,
      "label": "Bad Request",
      "description": "$description"
    }"""

  def resultList(apiPrefix: String,
                 pageSize: Int = 10,
                 totalPages: Int = 1,
                 totalResults: Int = 1) =
    s"""
      "@context": "https://localhost:8888/$apiPrefix/context.json",
      "type": "ResultList",
      "pageSize": $pageSize,
      "totalPages": $totalPages,
      "totalResults": $totalResults
    """

  def notFound(apiPrefix: String, description: String) =
    s"""{
      "@context": "https://localhost:8888/$apiPrefix/context.json",
      "type": "Error",
      "errorType": "http",
      "httpStatus": 404,
      "label": "Not Found",
      "description": "$description"
    }"""

  def gone(apiPrefix: String) =
    s"""{
      "@context": "https://localhost:8888/$apiPrefix/context.json",
      "type": "Error",
      "errorType": "http",
      "httpStatus": 410,
      "label": "Gone",
      "description": "This work has been deleted"
    }"""
}
