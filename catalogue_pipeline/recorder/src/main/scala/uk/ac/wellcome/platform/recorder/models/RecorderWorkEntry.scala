package uk.ac.wellcome.platform.recorder.models

import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.models.work.internal.UnidentifiedWork

case class RecorderWorkEntry(
  sourceId: String,
  sourceName: String,
  work: UnidentifiedWork
) extends Sourced

case object RecorderWorkEntry {
  def apply(work: UnidentifiedWork): RecorderWorkEntry = RecorderWorkEntry(
    sourceId = work.sourceIdentifier.identifierScheme.toString,
    sourceName = work.sourceIdentifier.value,
    work = work
  )
}
