package uk.ac.wellcome.platform.api.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.sksamuel.elastic4s.http.search.SearchHit
import com.sksamuel.elastic4s.http.get.GetResponse
import uk.ac.wellcome.models._
import uk.ac.wellcome.utils.JsonUtil

case class WorksIncludes(identifiers: Boolean = false)

case class DisplayWork(id: String,
                       label: String,
                       description: Option[String] = None,
                       lettering: Option[String] = None,
                       createdDate: Option[Period] = None,
                       creators: List[Agent] = List(),
                       identifiers: Option[List[DisplayIdentifier]] = None) {
  @JsonProperty("type") val ontologyType: String = "Work"
}

case object DisplayWork {

  def apply(hit: SearchHit): DisplayWork = apply(hit, includes = WorksIncludes())

  def apply(hit: SearchHit, includes: WorksIncludes): DisplayWork = {
    jsonToDisplayWork(hit.sourceAsString, includes)
  }

  def apply(got: GetResponse, includes: WorksIncludes): DisplayWork = {
    jsonToDisplayWork(got.sourceAsString, includes)
  }

  private def jsonToDisplayWork(document: String, includes: WorksIncludes) = {
    val identifiedWork =
      JsonUtil.fromJson[IdentifiedWork](document).get

    DisplayWork(
      id = identifiedWork.canonicalId,
      label = identifiedWork.work.label,
      description = identifiedWork.work.description,
      lettering = identifiedWork.work.lettering,
      createdDate = identifiedWork.work.createdDate,
      // Wrapping this in Option to catch null value from Jackson
      creators = Option(identifiedWork.work.creators).getOrElse(Nil),
      identifiers =
        if (includes.identifiers)
          Some(identifiedWork.work.identifiers.map(DisplayIdentifier(_)))
        else None
    )
  }
}

case class DisplayIdentifier(source: String, name: String, value: String) {
  @JsonProperty("type")
  val ontologyType: String = "Identifier"
}

object DisplayIdentifier {
  def apply(sourceIdentifier: SourceIdentifier): DisplayIdentifier =
    DisplayIdentifier(source = sourceIdentifier.source,
                      name = sourceIdentifier.sourceId,
                      value = sourceIdentifier.value)
}
