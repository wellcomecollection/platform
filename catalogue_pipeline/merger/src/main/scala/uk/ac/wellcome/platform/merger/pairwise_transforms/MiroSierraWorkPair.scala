package uk.ac.wellcome.platform.merger.pairwise_transforms
import uk.ac.wellcome.models.work.internal.UnidentifiedWork

/** If we have a Miro work and a Sierra work with a single item,
  * the Sierra work replaces the Miro work (because this is metadata
  * that we can update).
  *
  * Specifically:
  *
  *   - We copy across all the identifiers from the Miro work (except
  *     those which contain Sierra identifiers)
  *   - We copy across the Miro location to the Sierra work *if* the
  *     Sierra work doesn't already have a digital location.
  *     In this case, the Sierra digital location supersedes the one
  *     from Miro.
  *
  */
object MiroSierraWorkPair extends PairwiseMerger {
  def mergeAndRedirectWork(
    miroWork: UnidentifiedWork,
    sierraWork: UnidentifiedWork): Option[PairwiseResult] =
    None
}
