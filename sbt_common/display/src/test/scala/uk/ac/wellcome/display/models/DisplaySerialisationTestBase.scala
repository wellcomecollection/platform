package uk.ac.wellcome.display.models

import io.circe.Json
import io.circe.parser._
import org.scalatest.Suite
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.utils.JsonUtil._

trait DisplaySerialisationTestBase { this: Suite =>

  def optionalString(fieldName: String, maybeStringValue: Option[String]) =
    maybeStringValue match {
      case None => ""
      case Some(p) =>
        s"""
           "$fieldName": "$p"
         """
    }

  def optionalObject[T](fieldName: String,
                        formatter: T => String,
                        maybeObjectValue: Option[T],
                        firstField: Boolean = false) =
    maybeObjectValue match {
      case None => ""
      case Some(o) =>
        s"""
           ${if (!firstField) ","}"$fieldName": ${formatter(o)}
         """
    }

  def items(identifiedItems: List[Identified[Item]]) =
    identifiedItems
      .map { it =>
        s"""{
          "id": "${it.canonicalId}",
          "type": "${it.agent.ontologyType}",
          "locations": [
            ${locations(it.agent.locations)}
          ]
        }"""
      }
      .mkString(",")

  def locations(locations: List[Location]) =
    locations
      .map {
        location(_)
      }
      .mkString(",")

  def location(loc: Location) =
    loc match {
      case l: DigitalLocation  => digitalLocation(l)
      case l: PhysicalLocation => physicalLocation(l)
    }

  def digitalLocation(digitalLocation: DigitalLocation) =
    s"""{
      "type": "${digitalLocation.ontologyType}",
      "locationType": ${locationType(digitalLocation.locationType)},
      "url": "${digitalLocation.url}"
      ${optionalObject("license", license, digitalLocation.license)}
    }"""

  def physicalLocation(loc: PhysicalLocation) =
    s"""
       {
        "type": "${loc.ontologyType}",
        "locationType": ${locationType(loc.locationType)},
        "label": "${loc.label}"
       }
     """

  def locationType(locType: LocationType): String

  def license(license: License): String

  def identifier(identifier: SourceIdentifier): String

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
      case ag: Unidentifiable[T] => serialise(ag.agent)
      case disp: Identified[T] =>
        val agent = parse(serialise(disp.agent)).right.get.asObject.get
        val identifiersJson = disp.identifiers.map { sourceIdentifier =>
          parse(identifier(sourceIdentifier)).right.get
        }
        val newJson = ("id", Json.fromString(disp.canonicalId)) +: (
          "identifiers",
          Json.arr(identifiersJson: _*)) +: agent
        Json.fromJsonObject(newJson).spaces2
    }

  def abstractAgent(ag: AbstractAgent) =
    ag match {
      case a: Agent        => agent(a)
      case o: Organisation => organisation(o)
      case p: Person       => person(p)
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

  def period(p: Period) =
    s"""{
      "type": "Period",
      "label": "${p.label}"
    }"""

  def concept(concept: AbstractConcept) =
    s"""
    {
      "type": "${concept.ontologyType}",
      "label": "${concept.label}"
    }
    """

  def concepts(concepts: List[Displayable[AbstractConcept]]) =
    concepts
      .map { c =>
        identifiedOrUnidentifiable(c, concept)
      }
      .mkString(",")

  def subject(s: Subject[Displayable[AbstractConcept]]) =
    s"""
    {
      "label": "${s.label}",
      "type" : "${s.ontologyType}",
      "concepts": [ ${concepts(s.concepts)} ]
    }
    """

  def subjects(subjects: List[Subject[Displayable[AbstractConcept]]]) =
    subjects
      .map { subject(_) }
      .mkString(",")

  def genre(g: Genre[Displayable[AbstractConcept]]) =
    s"""
    {
      "label": "${g.label}",
      "type" : "${g.ontologyType}",
      "concepts": [ ${concepts(g.concepts)} ]
    }
    """

  def genres(genres: List[Genre[Displayable[AbstractConcept]]]) =
    genres
      .map { genre(_) }
      .mkString(",")

  def contributor(c: Contributor[Displayable[AbstractAgent]]) =
    s"""
       |{
       |  "agent": ${identifiedOrUnidentifiable(c.agent, abstractAgent)},
       |  "roles": ${toJson(c.roles).get},
       |  "type": "Contributor"
       |}
     """.stripMargin

  def workType(w: WorkType) =
    s"""
       |{
       |  "id": "${w.id}",
       |  "label": "${w.label}",
       |  "type": "WorkType"
       |}
     """.stripMargin
}
