package uk.ac.wellcome.platform.merger.pairwise

import uk.ac.wellcome.models.work.internal.UnidentifiedWork

trait PairwiseMerger {
  def mergeAndRedirectWork(work1: UnidentifiedWork,
                           work2: UnidentifiedWork): Option[PairwiseResult]
}
