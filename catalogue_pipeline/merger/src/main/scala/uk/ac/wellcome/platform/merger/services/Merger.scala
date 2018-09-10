package uk.ac.wellcome.platform.merger.services

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.rules.SierraPhysicalDigitalMergeRule

trait MergerRules {
  def merge(works: Seq[UnidentifiedWork]): Seq[BaseWork]
}

class Merger extends Logging with MergerRules {
  def merge(works: Seq[UnidentifiedWork]): Seq[BaseWork] =
    SierraPhysicalDigitalMergeRule.mergeAndRedirectWork(works)

}
