package uk.ac.wellcome.models.work.internal

/** A representation of a work in our ontology */
trait Work {
  val sourceIdentifier: SourceIdentifier
  val identifiers: List[SourceIdentifier]
  val mergeCandidates: List[MergeCandidate]

  val workType: Option[WorkType]
  val title: Option[String]
  val description: Option[String]
  val physicalDescription: Option[String]
  val extent: Option[String]
  val lettering: Option[String]
  val createdDate: Option[Period]
  val subjects: List[Subject[IdentityState[AbstractConcept]]]
  val genres: List[Genre[IdentityState[AbstractConcept]]]
  val contributors: List[Contributor[IdentityState[AbstractAgent]]]
  val thumbnail: Option[Location]
  val production: List[ProductionEvent[IdentityState[AbstractAgent]]]
  val language: Option[Language]
  val dimensions: Option[String]

  val version: Int
  val visible: Boolean

  val ontologyType: String
}

case class UnidentifiedWork(
  sourceIdentifier: SourceIdentifier,
  identifiers: List[SourceIdentifier] = List(),
  mergeCandidates: List[MergeCandidate] = List(),

  title: Option[String],
  workType: Option[WorkType] = None,
  description: Option[String] = None,
  physicalDescription: Option[String] = None,
  extent: Option[String] = None,
  lettering: Option[String] = None,
  createdDate: Option[Period] = None,
  subjects: List[Subject[MaybeDisplayable[AbstractConcept]]] = Nil,
  genres: List[Genre[MaybeDisplayable[AbstractConcept]]] = Nil,
  contributors: List[Contributor[MaybeDisplayable[AbstractAgent]]] = Nil,
  thumbnail: Option[Location] = None,
  items: List[UnidentifiedItem] = Nil,
  production: List[ProductionEvent[MaybeDisplayable[AbstractAgent]]] = Nil,
  language: Option[Language] = None,
  dimensions: Option[String] = None,

  version: Int,
  visible: Boolean = true,

  ontologyType: String = "Work") extends Work

case class IdentifiedWork(
  canonicalId: String,
  title: Option[String],
  sourceIdentifier: SourceIdentifier,
  mergeCandidates: List[MergeCandidate] = List(),

  identifiers: List[SourceIdentifier] = List(),
  workType: Option[WorkType] = None,
  description: Option[String] = None,
  physicalDescription: Option[String] = None,
  extent: Option[String] = None,
  lettering: Option[String] = None,
  createdDate: Option[Period] = None,
  subjects: List[Subject[Displayable[AbstractConcept]]] = Nil,
  genres: List[Genre[Displayable[AbstractConcept]]] = Nil,
  contributors: List[Contributor[Displayable[AbstractAgent]]] = Nil,
  thumbnail: Option[Location] = None,
  items: List[IdentifiedItem] = Nil,
  production: List[ProductionEvent[Displayable[AbstractAgent]]] = Nil,
  language: Option[Language] = None,
  dimensions: Option[String] = None,

  version: Int,
  visible: Boolean = true,

  ontologyType: String = "Work") extends Work
