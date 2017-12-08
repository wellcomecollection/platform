package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.sksamuel.elastic4s.Indexable
import uk.ac.wellcome.utils.JsonUtil

/** A representation of a work in our ontology */
case class Work(title: String,
                identifiers: List[SourceIdentifier] = List(),
                canonicalId: Option[String] = None,
                description: Option[String] = None,
                lettering: Option[String] = None,
                createdDate: Option[Period] = None,
                subjects: List[Concept] = Nil,
                creators: List[Agent] = Nil,
                genres: List[Concept] = Nil,
                thumbnail: Option[Location] = None,
                items: List[Item] = Nil)
    extends Identifiable {
  @JsonProperty("type") val ontologyType: String = "Work"
}

case object Work extends Indexable[Work] {
  override def json(t: Work): String =
    JsonUtil.toJson(t).get
}

case class SourcedWork(sourceIdentifier: SourceIdentifier, work: Work)
