package uk.ac.wellcome.platform.api.works

import com.sksamuel.elastic4s.Indexable
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.{DisplayJacksonModuleTestBase, WorksUtil}
import uk.ac.wellcome.elasticsearch.test.utils.IndexedElasticSearchLocal
import uk.ac.wellcome.models._
import uk.ac.wellcome.platform.api.Server
import uk.ac.wellcome.utils.JsonUtil._

trait ApiWorksTestBase
    extends FunSpec
    with FeatureTestMixin
    with IndexedElasticSearchLocal
    with DisplayJacksonModuleTestBase
    with WorksUtil {

  val indexName = "works"
  val itemType = "work"

  implicit object IdentifiedWorkIndexable extends Indexable[IdentifiedWork] {
    override def json(t: IdentifiedWork): String =
      toJson(t).get
  }

  implicit val jsonMapper = IdentifiedWork
  override val server =
    new EmbeddedHttpServer(
      new Server,
      flags = Map(
        "es.host" -> "localhost",
        "es.port" -> 9200.toString,
        "es.name" -> "wellcome",
        "es.index" -> indexName,
        "es.type" -> itemType
      )
    )

  val apiPrefix = "catalogue/v1"

  val emptyJsonResult = s"""
    |{
    |  "@context": "https://localhost:8888/$apiPrefix/context.json",
    |  "type": "ResultList",
    |  "pageSize": 10,
    |  "totalPages": 0,
    |  "totalResults": 0,
    |  "results": []
    |}""".stripMargin

  def badRequest(description: String) =
    s"""{
      "@context": "https://localhost:8888/$apiPrefix/context.json",
      "type": "Error",
      "errorType": "http",
      "httpStatus": 400,
      "label": "Bad Request",
      "description": "$description"
    }"""

  def resultList(pageSize: Int = 10,
                 totalPages: Int = 1,
                 totalResults: Int = 1) =
    s"""
      "@context": "https://localhost:8888/$apiPrefix/context.json",
      "type": "ResultList",
      "pageSize": $pageSize,
      "totalPages": $totalPages,
      "totalResults": $totalResults
    """

  def notFound(description: String) =
    s"""{
      "@context": "https://localhost:8888/$apiPrefix/context.json",
      "type": "Error",
      "errorType": "http",
      "httpStatus": 404,
      "label": "Not Found",
      "description": "$description"
    }"""

  def gone =
    s"""{
      "@context": "https://localhost:8888/$apiPrefix/context.json",
      "type": "Error",
      "errorType": "http",
      "httpStatus": 410,
      "label": "Gone",
      "description": "This work has been deleted"
    }"""
}
