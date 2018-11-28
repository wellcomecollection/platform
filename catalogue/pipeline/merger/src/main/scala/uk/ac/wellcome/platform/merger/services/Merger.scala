package uk.ac.wellcome.platform.merger.services

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.rules.physicaldigital.SierraPhysicalDigitalMergeRule
import uk.ac.wellcome.platform.merger.rules.singlepagemiro.SierraMiroMergeRule

trait MergerRules {
  def merge(works: Seq[UnidentifiedWork]): Seq[BaseWork]
}

class Merger extends MergerRules {
  def merge(works: Seq[UnidentifiedWork]): Seq[BaseWork] = {
    (SierraPhysicalDigitalMergeRule.mergeAndRedirectWorks _ andThen SierraMiroMergeRule.mergeAndRedirectWorks)(
      works)
  }
}
