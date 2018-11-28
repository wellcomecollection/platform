package uk.ac.wellcome.platform.matcher.models

import uk.ac.wellcome.models.work.internal.UnidentifiedWork

case class WorkUpdate(workId: String,
                      version: Int,
                      referencedWorkIds: Set[String])

case object WorkUpdate {
  def apply(work: UnidentifiedWork): WorkUpdate = {
    val id = work.sourceIdentifier.toString
    val referencedWorkIds = work.mergeCandidates
      .map { mergeCandidate =>
        mergeCandidate.identifier.toString
      }
      .filterNot { _ == id }
      .toSet

    WorkUpdate(id, work.version, referencedWorkIds)
  }
}
