package uk.ac.wellcome.platform.matcher.models

import uk.ac.wellcome.models.work.internal.{SourceIdentifier, UnidentifiedWork}

// The matcher gets a notification when a new Work is written to the recorder
// table.  This model holds the identifiers on the new work.
//
case class LinkedWorkUpdate(sourceId: String, otherIds: Set[String])

case object LinkedWorkUpdate {
  def apply(work: UnidentifiedWork): LinkedWorkUpdate = {
    val sourceId = identifierToString(work.sourceIdentifier)
    val otherIds = work.identifiers
      .map { identifierToString(_) }
      .filterNot { _ == sourceId }
      .toSet

    LinkedWorkUpdate(sourceId, otherIds)
  }

  // Should this live on SourceIdentifier?
  private def identifierToString(sourceIdentifier: SourceIdentifier): String =
    s"${sourceIdentifier.identifierType.id}/${sourceIdentifier.value}"
}
