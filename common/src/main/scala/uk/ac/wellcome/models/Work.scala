package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.sksamuel.elastic4s.Indexable
import uk.ac.wellcome.utils.JsonUtil

/** An identifier received from one of the original sources */
case class SourceIdentifier(identifierScheme: String, value: String)

/** A representation of a work in our ontology */
case class Work(
  canonicalId: Option[String] = None,
  identifiers: List[SourceIdentifier],
  title: String,
  description: Option[String] = None,
  lettering: Option[String] = None,
  createdDate: Option[Period] = None,
  subjects: List[Concept] = List(),
  creators: List[Agent] = List(),
  genres: List[Concept] = List(),
  thumbnail: Option[Location] = None,
  items: List[Item] = List()
) extends Identifiable {
  @JsonProperty("type") val ontologyType: String = "Work"
}

object Work extends Indexable[Work] {
  override def json(t: Work): String =
    JsonUtil.toJson(t).get
}
