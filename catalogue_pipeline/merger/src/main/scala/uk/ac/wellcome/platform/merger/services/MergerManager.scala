package uk.ac.wellcome.platform.merger.services

import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{BaseWork, UnidentifiedWork}

object MergerManager {

  /** Given a list of recorder work entries retrieved from VHS, and a
    * merging function, apply the function to these works.
    *
    * If we got an incomplete list of results from VHS (for example,
    * wrong versions), we skip the merge and return the original works.
    */
  def applyMerge(
    maybeWorkEntries: List[Option[RecorderWorkEntry]],
    process: Seq[UnidentifiedWork] => Seq[BaseWork]): Seq[BaseWork] = {
    val workEntries = maybeWorkEntries.flatten
    val works = workEntries
      .map { _.work }
      .collect { case unidentifiedWork: UnidentifiedWork => unidentifiedWork }

    if (works.size == maybeWorkEntries.size) {
      process(works)
    } else {
      works
    }
  }
}
