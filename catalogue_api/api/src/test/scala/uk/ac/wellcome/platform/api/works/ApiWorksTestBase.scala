package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.FunSpec
import uk.ac.wellcome.models._
import uk.ac.wellcome.platform.api.{Server, WorksUtil}
import uk.ac.wellcome.test.utils.IndexedElasticSearchLocal

class ApiWorksTestBase
    extends FunSpec
    with FeatureTestMixin
    with IndexedElasticSearchLocal
    with WorksUtil {

  val indexName = "works"
  val itemType = "work"

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

  def items(its: List[Item]) =
    its
      .map { it =>
        s"""{
          "id": "${it.canonicalId.get}",
          "type": "${it.ontologyType}",
          "locations": [
            ${locations(it.locations)}
          ]
        }"""
      }
      .mkString(",")

  def locations(locations: List[Location]) =
    locations
      .map { location(_) }
      .mkString(",")

  def location(loc: Location) = loc match {
    case l: DigitalLocation => digitalLocation(l)
    case l: PhysicalLocation => physicalLocation(l)
  }

  def digitalLocation(loc: DigitalLocation) =
    s"""{
      "type": "${loc.ontologyType}",
      "locationType": "${loc.locationType}",
      "url": "${loc.url}",
      "license": ${license(loc.license)}
    }"""

  def physicalLocation(loc: PhysicalLocation) =
    s"""
       {
        "type": "${loc.ontologyType}",
        "locationType": "${loc.locationType}",
        "label": "${loc.label}"
       }
     """

  def license(license: License) =
    s"""{
      "label": "${license.label}",
      "licenseType": "${license.licenseType}",
      "type": "${license.ontologyType}",
      "url": "${license.url}"
    }"""

  def identifier(identifier: SourceIdentifier) =
    s"""{
      "type": "Identifier",
      "identifierScheme": "${identifier.identifierScheme}",
      "value": "${identifier.value}"
    }"""

  def agent(ag: Agent) =
    s"""{
      "type": "Agent",
      "label": "${ag.label}"
    }"""

  def period(p: Period) =
    s"""{
      "type": "Period",
      "label": "${p.label}"
    }"""

  def badRequest(description: String) =
    s"""{
      "@context": "https://localhost:8888/$apiPrefix/context.json",
      "type": "Error",
      "errorType": "http",
      "httpStatus": 400,
      "label": "Bad Request",
      "description": "$description"
    }"""

  def concept(con: Concept) =
    s"""
    {
      "type": "${con.ontologyType}",
      "label": "${con.label}"
    }
    """

  def concepts(concepts: List[Concept]) =
    concepts
      .map { concept(_) }
      .mkString(",")

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
