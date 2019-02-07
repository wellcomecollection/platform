package uk.ac.wellcome.platform.api.works

import com.sksamuel.elastic4s.{Index, Indexable}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.fixtures._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.IdentifiedWork
import uk.ac.wellcome.platform.api.Server

import scala.concurrent.ExecutionContext.Implicits.global

trait ApiWorksTestBase
    extends FunSpec
    with ElasticsearchFixtures
    with WorksGenerators {

  implicit object IdentifiedWorkIndexable extends Indexable[IdentifiedWork] {
    override def json(t: IdentifiedWork): String =
      toJson(t).get
  }

  def withServer[R](indexV1: Index, indexV2: Index)(
    testWith: TestWith[EmbeddedHttpServer, R]): R = {

    val server: EmbeddedHttpServer = new EmbeddedHttpServer(
      new Server,
      flags = displayEsLocalFlags(
        indexV1 = indexV1,
        indexV2 = indexV2
      )
    )

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }

  val apiName = "catalogue/"

  def getApiPrefix(
    apiVersion: ApiVersions.Value = ApiVersions.default): String =
    apiName + apiVersion

  def withApi[R](testWith: TestWith[(Index, Index, EmbeddedHttpServer), R]): R =
    withLocalWorksIndex { indexV1 =>
      withLocalWorksIndex { indexV2 =>
        withServer(indexV1, indexV2) { server =>
          testWith((indexV1, indexV2, server))
        }
      }
    }

  def withHttpServer[R](testWith: TestWith[EmbeddedHttpServer, R]): R =
    withServer(Index("index-v1"), Index("index-v2")) { server =>
      testWith(server)
    }

  def emptyJsonResult(apiPrefix: String): String = s"""
    |{
    |  ${resultList(apiPrefix, totalPages = 0, totalResults = 0)},
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

  def withEmptyIndex[R]: Fixture[Index, R] =
    fixture[Index, R](
      create = {
        val index = Index(randomAlphanumeric(length = 10))
        elasticClient
          .execute {
            createIndex(index.name)
          }
        eventuallyIndexExists(index)
        index
      },
      destroy = eventuallyDeleteIndex
    )
}
