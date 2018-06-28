package uk.ac.wellcome.models.work.internal

sealed trait Redirect
case class IdentifiableRedirect(sourceIdentifier: SourceIdentifier)
    extends Redirect
case class IdentifiedRedirect(canonicalId: String) extends Redirect
