package uk.ac.wellcome.platform.api.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.sksamuel.elastic4s.searches.RichSearchHit
import org.elasticsearch.action.get.GetResponse
import uk.ac.wellcome.models._
import uk.ac.wellcome.utils.JsonUtil

case class DisplayWork(
  id: String,
  label: String,
  description: Option[String] = None,
  lettering: Option[String] = None,
  hasCreatedDate: Option[Period] = None,
  hasCreator: List[Agent] = List(),
  hasIdentifier: Option[List[DisplayIdentifier]] = None) {
  @JsonProperty("type") val ontologyType: String = "Work"
}

case object DisplayWork {

  def apply(hit: RichSearchHit): DisplayWork = apply(hit, includes = List())

  def apply(hit: RichSearchHit, includes: List[String]): DisplayWork = {
    jsonToDisplayWork(hit.sourceAsString, includes)
  }

  def apply(got: GetResponse, includes: List[String]): DisplayWork = {
    jsonToDisplayWork(got.getSourceAsString, includes)
  }

  private def jsonToDisplayWork(document: String, includes: List[String]) = {
    val identifiedWork =
      JsonUtil.fromJson[IdentifiedWork](document).get

    DisplayWork(
      id = identifiedWork.canonicalId,
      label = identifiedWork.work.label,
      description = identifiedWork.work.description,
      lettering = identifiedWork.work.lettering,
      hasCreatedDate = identifiedWork.work.hasCreatedDate,
      // Wrapping this in Option to catch null value from Jackson
      hasCreator = Option(identifiedWork.work.hasCreator).getOrElse(Nil),
      hasIdentifier =
        if (includes.contains("hasIdentifier"))
          Some(identifiedWork.work.identifiers.map(DisplayIdentifier(_)))
        else None
    )
  }
}

case class DisplayIdentifier(source: DisplaySource, value: String) {
  @JsonProperty("type")
  val ontologyType: String = "Identifier"
}

object DisplayIdentifier {
  def apply(sourceIdentifier: SourceIdentifier): DisplayIdentifier =
    DisplayIdentifier(source =
                        DisplaySource(name = sourceIdentifier.source,
                                      value = sourceIdentifier.sourceId),
                      value = sourceIdentifier.value)
}

case class DisplaySource(name: String, value: String)
