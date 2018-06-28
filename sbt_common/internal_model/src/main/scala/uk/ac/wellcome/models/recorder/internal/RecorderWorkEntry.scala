package uk.ac.wellcome.models.recorder.internal

import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.models.work.internal.TransformedBaseWork

case class RecorderWorkEntry(
  sourceId: String,
  sourceName: String,
  work: TransformedBaseWork
) extends Sourced

case object RecorderWorkEntry {
  def apply(work: TransformedBaseWork): RecorderWorkEntry = RecorderWorkEntry(
    sourceId = work.sourceIdentifier.value,
    sourceName = work.sourceIdentifier.identifierType.id,
    work = work
  )
}
