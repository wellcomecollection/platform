package uk.ac.wellcome.platform.api.works

import com.sksamuel.elastic4s.Indexable
import com.twitter.finagle.http.Status
import org.scalatest.FunSpec
import uk.ac.wellcome.models._
import uk.ac.wellcome.platform.api.{Server, WorksUtil}
import uk.ac.wellcome.platform.api.fixtures
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.utils.JsonUtil._

class ApiWorksTestBase
    extends FunSpec
    with fixtures.Server
    with ElasticsearchFixtures
    with WorksUtil {

  val itemType = "work"

  implicit object IdentifiedWorkIndexable extends Indexable[IdentifiedWork] {
    override def json(t: IdentifiedWork): String =
      toJson(t).get
  }

  implicit val jsonMapper = IdentifiedWork

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

  def items(its: List[IdentifiedItem]) =
    its
      .map { it =>
        s"""{
          "id": "${it.canonicalId}",
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

  def abstractAgent(ag: AbstractAgent) =
    ag match {
      case a: Agent => agent(a)
      case o: Organisation => organisation(o)
      case p: Person => person(p)
    }

  def person(p: Person) = {
    s"""{
        "type": "Person",
        ${optionalString("prefix", p.prefix)},
        ${optionalString("numeration", p.numeration)},
        "label": "${p.label}"
      }"""
  }

  def organisation(o: Organisation) = {
    s"""{
        "type": "Organisation",
        "label": "${o.label}"
      }"""
  }

  def agent(a: Agent) = {
    s"""{
        "type": "Agent",
        "label": "${a.label}"
      }"""
  }

  def optionalString(fieldName: String, maybeValue: Option[String]) =
    maybeValue match {
      case None => ""
      case Some(p) =>
        s"""
           "$fieldName": "$p"
         """
    }

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

  def concept(con: AbstractConcept) =
    s"""
    {
      "type": "${con.ontologyType}",
      "label": "${con.label}"
    }
    """

  def concepts(concepts: List[AbstractConcept]) =
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
