package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.sksamuel.elastic4s.Indexable
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.utils.JsonUtil.toJson

/** A representation of a work in our ontology */
case class Work(title: Option[String],
                sourceIdentifier: SourceIdentifier,
                identifiers: List[SourceIdentifier] = List(),
                canonicalId: Option[String] = None,
                description: Option[String] = None,
                lettering: Option[String] = None,
                createdDate: Option[Period] = None,
                subjects: List[Concept] = Nil,
                creators: List[Agent] = Nil,
                genres: List[Concept] = Nil,
                thumbnail: Option[Location] = None,
                items: List[Item] = Nil,
                publishers: List[AbstractAgent] = Nil,
                visible: Boolean = true,
                @JsonProperty("type") ontologyType: String = "Work")
    extends Identifiable

case object Work extends Indexable[Work] {
  override def json(t: Work): String =
    toJson(t).get
}
