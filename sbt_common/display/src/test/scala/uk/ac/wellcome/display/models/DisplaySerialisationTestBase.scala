package uk.ac.wellcome.display.models

import io.circe.Json
import io.circe.parser._
import org.scalatest.Suite
import uk.ac.wellcome.models._
import uk.ac.wellcome.utils.JsonUtil._

trait DisplaySerialisationTestBase { this: Suite =>

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

  // Some of our fields can be optionally identified (e.g. creators).
  //
  // Values in these fields are wrapped in either "Unidentifiable" or
  // "Identified".  In the first case, we use the default serialisation
  // unmodified.  In the second case, we modify the JSON to include
  // the "id" field and the "identifiers" field.
  //
  def identifiedOrUnidentifiable[T](displayable: Displayable[T],
                                    serialise: T => String) =
    displayable match {
      case Unidentifiable(ag) => serialise(ag)
      case Identified(ag, id, identifiers) =>
        val agent = parse(serialise(ag)).right.get.asObject.get
        val identifiersJson = identifiers.map { sourceIdentifier =>
          parse(identifier(sourceIdentifier)).right.get
        }
        val newJson = ("id", Json.fromString(id)) +: (
          "identifiers",
          Json.arr(identifiersJson: _*)) +: agent
        Json.fromJsonObject(newJson).spaces2
    }

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

  def subject(s: Subject) =
    s"""
  {
    "label": "${s.label}",
    "type" : "${s.ontologyType}",
    "concepts": [ ${concepts(s.concepts)} ]
   }
   """

  def subjects(subjects: List[Subject]) =
    subjects
      .map { subject(_) }
      .mkString(",")

  def contributor(c: Contributor[Displayable[AbstractAgent]]) =
    s"""
       |{
       |  "agent": ${identifiedOrUnidentifiable(c.agent, abstractAgent)},
       |  "roles": ${toJson(c.roles).get},
       |  "type": "Contributor"
       |}
     """.stripMargin

}
