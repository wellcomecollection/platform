package uk.ac.wellcome.platform.api.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.sksamuel.elastic4s.searches.RichSearchHit
import org.elasticsearch.action.get.GetResponse
import uk.ac.wellcome.models.{Agent, IdentifiedWork, Period, Work}
import uk.ac.wellcome.utils.JsonUtil

case class DisplayWork(
  @JsonProperty("type") ontologyType: String = "Work",
  id: String,
  label: String,
  description: Option[String] = None,
  lettering: Option[String] = None,
  hasCreatedDate: Option[Period] = None,
  hasCreator: List[Agent] = List()
)
case object DisplayWork {
  def apply(hit: RichSearchHit): DisplayWork = {
    toRecord(hit.sourceAsString)
  }

  def apply(got: GetResponse): DisplayWork = {
    toRecord(got.getSourceAsString)
  }

  private def toRecord(document: String) = {
    val identifiedWork =
      JsonUtil.fromJson[IdentifiedWork](document).get

    DisplayWork(
      id = identifiedWork.canonicalId,
      label = identifiedWork.work.label,
      description = identifiedWork.work.description,
      lettering = identifiedWork.work.lettering,
      hasCreatedDate = identifiedWork.work.hasCreatedDate,
      // Wrapping this in Option to catch null value from Jackson
      hasCreator = Option(identifiedWork.work.hasCreator).getOrElse(Nil)
    )
  }
}
