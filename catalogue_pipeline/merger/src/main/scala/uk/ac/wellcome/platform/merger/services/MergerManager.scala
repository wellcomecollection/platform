package uk.ac.wellcome.platform.merger.services

import com.google.inject.Inject
import uk.ac.wellcome.models.work.internal.{BaseWork, TransformedBaseWork, UnidentifiedWork}

class MergerManager @Inject()(
  mergerRules: MergerRules
) {

  /** Given a list of recorder work entries retrieved from VHS, and a
    * merging function, apply the function to these works.
    *
    * If we got an incomplete list of results from VHS (for example,
    * wrong versions), we skip the merge and return the original works.
    */
  def applyMerge(
    maybeWorks: List[Option[TransformedBaseWork]]): Seq[BaseWork] = {
    val works = maybeWorks.flatten
    val unidentifiedWorks = works
      .collect { case unidentifiedWork: UnidentifiedWork => unidentifiedWork }

    if (unidentifiedWorks.size == works.size) {
      mergerRules.merge(unidentifiedWorks)
    } else {
      works
    }
  }
}
