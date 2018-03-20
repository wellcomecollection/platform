package uk.ac.wellcome.models

import uk.ac.wellcome.utils.JsonUtil._

/** A representation of a work in our ontology */
trait Work extends Versioned {
  val title: Option[String]
  val sourceIdentifier: SourceIdentifier
  val version: Int
  val identifiers: List[SourceIdentifier]
  val workType: Option[WorkType]
  val description: Option[String]
  val physicalDescription: Option[String]
  val extent: Option[String]
  val lettering: Option[String]
  val createdDate: Option[Period]
  val subjects: List[AbstractConcept]
  val creators: List[IdentityState[AbstractAgent]]
  val genres: List[AbstractConcept]
  val thumbnail: Option[Location]
  val publishers: List[IdentityState[AbstractAgent]]
  val publicationDate: Option[Period]
  val language: Option[Language]
  val visible: Boolean
  val ontologyType: String
}

case class UnidentifiedWork(title: Option[String],
                            sourceIdentifier: SourceIdentifier,
                            version: Int,
                            identifiers: List[SourceIdentifier] = List(),
                            workType: Option[WorkType] = None,
                            description: Option[String] = None,
                            physicalDescription: Option[String] = None,
                            extent: Option[String] = None,
                            lettering: Option[String] = None,
                            createdDate: Option[Period] = None,
                            subjects: List[AbstractConcept] = Nil,
                            creators: List[IdentifieableOrUnidentifiable[AbstractAgent]] = Nil,
                            genres: List[AbstractConcept] = Nil,
                            thumbnail: Option[Location] = None,
                            items: List[UnidentifiedItem] = Nil,
                            publishers: List[IdentifieableOrUnidentifiable[AbstractAgent]] = Nil,
                            publicationDate: Option[Period] = None,
                            placesOfPublication: List[Place] = Nil,
                            language: Option[Language] = None,
                            visible: Boolean = true,
                            ontologyType: String = "Work")
    extends Work

case class IdentifiedWork(canonicalId: String,
                          title: Option[String],
                          sourceIdentifier: SourceIdentifier,
                          version: Int,
                          identifiers: List[SourceIdentifier] = List(),
                          workType: Option[WorkType] = None,
                          description: Option[String] = None,
                          physicalDescription: Option[String] = None,
                          extent: Option[String] = None,
                          lettering: Option[String] = None,
                          createdDate: Option[Period] = None,
                          subjects: List[AbstractConcept] = Nil,
                          creators: List[IdentifiedOrUnidentifiable[AbstractAgent]] = Nil,
                          genres: List[AbstractConcept] = Nil,
                          thumbnail: Option[Location] = None,
                          items: List[IdentifiedItem] = Nil,
                          publishers: List[IdentifiedOrUnidentifiable[AbstractAgent]] = Nil,
                          publicationDate: Option[Period] = None,
                          placesOfPublication: List[Place] = Nil,
                          language: Option[Language] = None,
                          visible: Boolean = true,
                          ontologyType: String = "Work")
    extends Work
