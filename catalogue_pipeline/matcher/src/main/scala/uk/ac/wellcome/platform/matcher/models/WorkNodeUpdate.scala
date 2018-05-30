package uk.ac.wellcome.platform.matcher.models

import uk.ac.wellcome.models.work.internal.{SourceIdentifier, UnidentifiedWork}

// This represents the identifiers on an individual Work.
//
// When we receive a Work from the recorder, we're only interested in the
// identifiers -- the rest of the Work is unimportant -- so we copy them
// out into a smaller model.
//
//  - id is the sourceIdentifier of the original work
//  - referencedWorkIds is all the other identifiers referred to in
//    the "identifiers" list which _aren't_ the sourceIdentifier
//
case class WorkNodeUpdate(id: String, referencedWorkIds: Set[String])

case object WorkNodeUpdate {
  def apply(work: UnidentifiedWork): WorkNodeUpdate = {
    val id = identifierToString(work.sourceIdentifier)
    val referencedWorkIds = work.identifiers
      .map { identifierToString(_) }
      .filterNot { _ == id }
      .toSet
    WorkNodeUpdate(id = id, referencedWorkIds = referencedWorkIds)
  }

  private def identifierToString(sourceIdentifier: SourceIdentifier): String =
    s"${sourceIdentifier.identifierType.id}/${sourceIdentifier.value}"
}
