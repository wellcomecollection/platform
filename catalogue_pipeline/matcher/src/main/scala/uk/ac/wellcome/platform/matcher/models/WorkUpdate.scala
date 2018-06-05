package uk.ac.wellcome.platform.matcher.models

import uk.ac.wellcome.models.work.internal.{SourceIdentifier, UnidentifiedWork}

case class WorkUpdate(workId: String,
                      version: Int,
                      referencedWorkIds: Set[String])

case object WorkUpdate {
  def apply(work: UnidentifiedWork): WorkUpdate = {
    val id = identifierToString(work.sourceIdentifier)
    val referencedWorkIds = work.identifiers
      .map { identifierToString(_) }
      .filterNot { _ == id }
      .toSet

    WorkUpdate(id, work.version, referencedWorkIds)
  }

  private def identifierToString(sourceIdentifier: SourceIdentifier): String =
    s"${sourceIdentifier.identifierType.id}/${sourceIdentifier.value}"
}
