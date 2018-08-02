package uk.ac.wellcome.models.work.internal

sealed trait BaseWork {
  val version: Int
  val sourceIdentifier: SourceIdentifier
}

sealed trait IdentifiedBaseWork extends BaseWork {
  val canonicalId: String
}
sealed trait TransformedBaseWork extends BaseWork

/** A representation of a work in our ontology */
sealed trait Work extends BaseWork with MultipleSourceIdentifiers {
  val sourceIdentifier: SourceIdentifier
  val otherIdentifiers: List[SourceIdentifier]
  val mergeCandidates: List[MergeCandidate]

  val title: String
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

  val items: List[IdentityState[Item[IdentityState[String]]]]

  val version: Int

  val ontologyType: String
}

case class UnidentifiedWork(
  sourceIdentifier: SourceIdentifier,
  otherIdentifiers: List[SourceIdentifier],
  mergeCandidates: List[MergeCandidate],
  title: String,
  workType: Option[WorkType],
  description: Option[String],
  physicalDescription: Option[String],
  extent: Option[String],
  lettering: Option[String],
  createdDate: Option[Period],
  subjects: List[Subject[MaybeDisplayable[AbstractConcept]]],
  genres: List[Genre[MaybeDisplayable[AbstractConcept]]],
  contributors: List[Contributor[MaybeDisplayable[AbstractAgent]]],
  thumbnail: Option[Location],
  production: List[ProductionEvent[MaybeDisplayable[AbstractAgent]]],
  language: Option[Language],
  dimensions: Option[String],
  items: List[MaybeDisplayable[Item[MaybeDisplayable[String]]]],
  version: Int,
  ontologyType: String = "Work",
  identifiedType: String = classOf[IdentifiedWork].getSimpleName)
    extends Work
    with TransformedBaseWork

case class IdentifiedWork(
  canonicalId: String,
  sourceIdentifier: SourceIdentifier,
  otherIdentifiers: List[SourceIdentifier] = List(),
  mergeCandidates: List[MergeCandidate] = List(),
  title: String,
  workType: Option[WorkType],
  description: Option[String],
  physicalDescription: Option[String],
  extent: Option[String],
  lettering: Option[String],
  createdDate: Option[Period],
  subjects: List[Subject[Displayable[AbstractConcept]]],
  genres: List[Genre[Displayable[AbstractConcept]]],
  contributors: List[Contributor[Displayable[AbstractAgent]]],
  thumbnail: Option[Location],
  production: List[ProductionEvent[Displayable[AbstractAgent]]],
  language: Option[Language],
  dimensions: Option[String],
  items: List[Displayable[Item[Displayable[String]]]],
  version: Int,
  ontologyType: String = "Work")
    extends Work
    with IdentifiedBaseWork

sealed trait InvisibleWork extends BaseWork

case class UnidentifiedInvisibleWork(
  sourceIdentifier: SourceIdentifier,
  version: Int,
  identifiedType: String = classOf[IdentifiedInvisibleWork].getSimpleName)
    extends InvisibleWork
    with TransformedBaseWork

case class IdentifiedInvisibleWork(sourceIdentifier: SourceIdentifier,
                                   version: Int,
                                   canonicalId: String)
    extends InvisibleWork
    with IdentifiedBaseWork

sealed trait RedirectedWork extends BaseWork {
  val redirect: Redirect
}

case class UnidentifiedRedirectedWork(
  sourceIdentifier: SourceIdentifier,
  version: Int,
  redirect: IdentifiableRedirect,
  identifiedType: String = classOf[IdentifiedRedirectedWork].getSimpleName
) extends RedirectedWork

case class IdentifiedRedirectedWork(
  canonicalId: String,
  sourceIdentifier: SourceIdentifier,
  version: Int,
  redirect: IdentifiedRedirect
) extends RedirectedWork
    with IdentifiedBaseWork
