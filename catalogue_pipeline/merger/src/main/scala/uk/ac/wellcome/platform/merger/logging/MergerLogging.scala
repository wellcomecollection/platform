package uk.ac.wellcome.platform.merger.logging

import uk.ac.wellcome.models.work.internal.UnidentifiedWork

trait MergerLogging {
  def describeWorkPair(workA: UnidentifiedWork, workB: UnidentifiedWork) =
    s"(id=${workA.sourceIdentifier.value}) and (id=${workB.sourceIdentifier.value})"

  def describeWorkPairWithItems(workA: UnidentifiedWork,
                                workB: UnidentifiedWork): String =
    s"(id=${workA.sourceIdentifier.value}, itemsCount=${workA.items.size}) and " +
      s"(id=${workB.sourceIdentifier.value}, itemsCount=${workB.items.size})"
}
