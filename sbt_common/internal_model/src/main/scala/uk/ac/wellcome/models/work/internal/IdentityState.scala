package uk.ac.wellcome.models.work.internal

sealed trait IdentityState[+T]

sealed trait MaybeDisplayable[+T] extends IdentityState[T] {
  val agent: T
}

sealed trait Displayable[+T] extends IdentityState[T]

case class Identified[T](agent: T,
                         canonicalId: String,
                         sourceIdentifier: SourceIdentifier,
                         otherIdentifiers: List[SourceIdentifier] = List())
    extends IdentityState[T]
    with Displayable[T]
    with MultipleSourceIdentifiers

case class Identifiable[T](agent: T,
                           sourceIdentifier: SourceIdentifier,
                           otherIdentifiers: List[SourceIdentifier] = List(),
                           identifiedType: String =
                             classOf[Identified[T]].getSimpleName)
    extends IdentityState[T]
    with MaybeDisplayable[T]
    with MultipleSourceIdentifiers

case class Unidentifiable[T](agent: T)
    extends IdentityState[T]
    with Displayable[T]
    with MaybeDisplayable[T]

/** This is used in cases where we do have source identifiers, but there
  * are multiple identifiers and it's not clear which one should be used
  * as the reference point for the ID minter.
  */
case class AmbiguouslyIdentified[T](agent: T,
                                    identifiers: List[SourceIdentifier] = List())
    extends IdentityState[T]
    with Displayable[T]
    with MaybeDisplayable[T]
