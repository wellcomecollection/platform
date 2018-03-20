package uk.ac.wellcome.models

sealed trait IdentityState[+T]

sealed trait IdentifieableOrUnidentifiable[+T] extends IdentityState[T]
sealed trait IdentifiedOrUnidentifiable[+T] extends IdentityState[T]

case class Identified[T](agent: T, canonicalId: String, identifiers: List[SourceIdentifier]) extends IdentityState[T] with IdentifiedOrUnidentifiable[T]
case class Identifiable[T](agent: T, sourceIdentifier: SourceIdentifier, identifiers: List[SourceIdentifier]) extends IdentityState[T] with IdentifieableOrUnidentifiable[T]
case class Unidentifiable[T](agent: T) extends IdentityState[T] with IdentifiedOrUnidentifiable[T] with IdentifieableOrUnidentifiable[T]
