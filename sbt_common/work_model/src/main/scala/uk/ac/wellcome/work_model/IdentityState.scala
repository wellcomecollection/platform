package uk.ac.wellcome.work_model

sealed trait IdentityState[+T]

sealed trait MaybeDisplayable[+T] extends IdentityState[T]
sealed trait Displayable[+T] extends IdentityState[T]

case class Identified[T](agent: T,
                         canonicalId: String,
                         identifiers: List[SourceIdentifier])
    extends IdentityState[T]
    with Displayable[T]

case class Identifiable[T](agent: T,
                           sourceIdentifier: SourceIdentifier,
                           identifiers: List[SourceIdentifier])
    extends IdentityState[T]
    with MaybeDisplayable[T]

case class Unidentifiable[T](agent: T)
    extends IdentityState[T]
    with Displayable[T]
    with MaybeDisplayable[T]
