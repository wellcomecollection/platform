package uk.ac.wellcome.models

import com.sksamuel.elastic4s.Indexable
import uk.ac.wellcome.utils.JsonUtil._

/** A representation of a work in our ontology */
trait Work extends Versioned with Identifiable {
  val title: Option[String]
  val sourceIdentifier: SourceIdentifier
  val version: Int
  val identifiers: List[SourceIdentifier]
  val description: Option[String]
  val lettering: Option[String]
  val createdDate: Option[Period]
  val subjects: List[Concept]
  val creators: List[Agent]
  val genres: List[Concept]
  val thumbnail: Option[Location]
  val publishers: List[AbstractAgent]
  val visible: Boolean
  val ontologyType: String
}

case class UnidentifiedWork(title: Option[String],
                            sourceIdentifier: SourceIdentifier,
                            version: Int,
                            identifiers: List[SourceIdentifier] = List(),
                            description: Option[String] = None,
                            lettering: Option[String] = None,
                            createdDate: Option[Period] = None,
                            subjects: List[Concept] = Nil,
                            creators: List[Agent] = Nil,
                            genres: List[Concept] = Nil,
                            thumbnail: Option[Location] = None,
                            items: List[UnidentifiedItem] = Nil,
                            publishers: List[AbstractAgent] = Nil,
                            visible: Boolean = true,
                            ontologyType: String = "Work",
                            publicationDate: Option[Period] = None)
    extends Work

case class IdentifiedWork(canonicalId: String,
                          title: Option[String],
                          sourceIdentifier: SourceIdentifier,
                          version: Int,
                          identifiers: List[SourceIdentifier] = List(),
                          description: Option[String] = None,
                          lettering: Option[String] = None,
                          createdDate: Option[Period] = None,
                          subjects: List[Concept] = Nil,
                          creators: List[Agent] = Nil,
                          genres: List[Concept] = Nil,
                          thumbnail: Option[Location] = None,
                          items: List[IdentifiedItem] = Nil,
                          publishers: List[AbstractAgent] = Nil,
                          visible: Boolean = true,
                          ontologyType: String = "Work")
    extends Work

case object IdentifiedWork extends Indexable[IdentifiedWork] {
  override def json(t: IdentifiedWork): String =
    toJson(t).get
}
