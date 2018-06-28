package uk.ac.wellcome.models.work.internal

sealed trait BaseWork {
  val version: Int
  val sourceIdentifier: SourceIdentifier
}

sealed trait IdentifiedBaseWork extends BaseWork
/** A representation of a work in our ontology */
trait Work extends BaseWork with MultipleSourceIdentifiers {
  val sourceIdentifier: SourceIdentifier
  val otherIdentifiers: List[SourceIdentifier]
  val mergeCandidates: List[MergeCandidate]

  val title: Option[String]
  val workType: Option[WorkType]
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

  val items: List[Item]

  val version: Int
  val visible: Boolean

  val ontologyType: String
}

case class UnidentifiedWork(
  sourceIdentifier: SourceIdentifier,
  version: Int,
  title: Option[String],
  otherIdentifiers: List[SourceIdentifier] = List(),
  mergeCandidates: List[MergeCandidate] = List(),
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
  production: List[ProductionEvent[MaybeDisplayable[AbstractAgent]]] = Nil,
  language: Option[Language] = None,
  dimensions: Option[String] = None,
  items: List[UnidentifiedItem] = Nil,
  visible: Boolean = true,
  ontologyType: String = "Work")
    extends Work with IdentifiedBaseWork

case class IdentifiedWork(
  canonicalId: String,
  sourceIdentifier: SourceIdentifier,
  otherIdentifiers: List[SourceIdentifier] = List(),
  mergeCandidates: List[MergeCandidate] = List(),
  title: Option[String],
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
  production: List[ProductionEvent[Displayable[AbstractAgent]]] = Nil,
  language: Option[Language] = None,
  dimensions: Option[String] = None,
  items: List[IdentifiedItem] = Nil,
  version: Int,
  visible: Boolean = true,
  ontologyType: String = "Work")
    extends Work

trait RedirectedWork extends BaseWork {
  val sourceIdentifier: SourceIdentifier
  val version: Int
  val redirect: Redirect
}

case class UnidentifiedRedirectedWork(
                                     sourceIdentifier: SourceIdentifier,
                                     version: Int,
                                     redirect: IdentifiableRedirect
                                     ) extends RedirectedWork

case class IdentifiedRedirectedWork(
                                   canonicalId: String,
                                     sourceIdentifier: SourceIdentifier,
                                     version: Int,
                                     redirect: IdentifiedRedirect
                                     ) extends RedirectedWork with IdentifiedBaseWork