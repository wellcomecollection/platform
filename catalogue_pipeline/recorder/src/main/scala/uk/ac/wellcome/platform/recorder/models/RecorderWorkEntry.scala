package uk.ac.wellcome.recorder.models

import uk.ac.wellcome.models.Id
import uk.ac.wellcome.models.work.internal.UnidentifiedWork

case class RecorderWorkEntry(
  id: String,
  work: UnidentifiedWork
) extends Id

case object RecorderWorkEntry {
  def apply(work: UnidentifiedWork): RecorderWorkEntry = RecorderWorkEntry(
    id = s"${work.sourceIdentifier.identifierScheme}/${work.sourceIdentifier.value}",
    work = work
  )
}
