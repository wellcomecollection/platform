package uk.ac.wellcome.platform.merger.pairwise

import uk.ac.wellcome.models.work.internal.{UnidentifiedRedirectedWork, UnidentifiedWork}

/** Represents the outcome of a pairwise merge.
  *
  * The [[mergedWork]] is the work with higher precedent, that we keep
  * after the merge.  The [[redirectedWork]] is the one which is deposed,
  * and will eventually redirect to the [[mergedWork]].
  */
case class PairwiseResult(
  mergedWork: UnidentifiedWork,
  redirectedWork: UnidentifiedRedirectedWork
)
