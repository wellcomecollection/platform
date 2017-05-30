package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.sksamuel.elastic4s.Indexable
import uk.ac.wellcome.utils.JsonUtil

/** Represents a set of identifiers as stored in DynamoDB */
case class Identifier(CanonicalID: String, MiroID: String)

/** An identifier received from one of the original sources */
case class SourceIdentifier(source: String, sourceId: String, value: String)

case class IdentifiedWork(canonicalId: String, work: Work)

/** A representation of a work in our ontology, without a
 *  canonical identifier.
 */
case class Work(
  identifiers: List[SourceIdentifier],
  label: String,
  description: Option[String] = None,
  lettering: Option[String] = None,
  createdDate: Option[Period] = None,
  creators: List[Agent] = List()
) {
  @JsonProperty("type") val ldType: String = "Work"
}

object IdentifiedWork extends Indexable[IdentifiedWork] {
  override def json(t: IdentifiedWork): String =
    JsonUtil.toJson(t).get
}
