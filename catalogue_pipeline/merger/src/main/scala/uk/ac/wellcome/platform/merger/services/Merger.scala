package uk.ac.wellcome.platform.merger.services

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.pairwise_transforms.SierraPhysicalDigitalWorkPair

trait MergerRules {
  def merge(works: Seq[UnidentifiedWork]): Seq[BaseWork]
}

/** The merger is implemented as a series of "pairwise" merges.
  *
  * Each of the pairwise mergers is implemented (and tested) as a separate object.
  * This class should _only_ contain logic for deciding whether there are any
  * works which are eligible for merging, and call the pairwise merger to do
  * the actual logic of merging.
  *
  * The merger currently knows about three types of work, in decreasing order
  * of precedence:
  *
  *   - Sierra physical record
  *   - Sierra digital record
  *   - Miro record
  *
  * We apply the pairwise merges in this order:
  *
  *     Miro + Sierra digital
  *     Miro + Sierra physical
  *     Sierra digital + Sierra physical
  *
  * Each of these pairwise merges is implemented as a single method on
  * this class, and each method should take the list of current works,
  * and return the remaining works after applying this transformation.
  *
  */
class Merger extends Logging with MergerRules {
  def merge(works: Seq[UnidentifiedWork]): Seq[BaseWork] =
    maybeMergePhysicalDigitalWorkPair(works)

  /** If we have a single physical Sierra work and a single digital Sierra work,
    * we can merge them into a single work.
    */
  def maybeMergePhysicalDigitalWorkPair(
    works: Seq[UnidentifiedWork]): Seq[BaseWork] = {
    val physicalWorks = works.filter { isSierraPhysicalWork }
    val digitalWorks = works.filter { isSierraDigitalWork }

    (physicalWorks, digitalWorks) match {
      case (List(physicalWork), List(digitalWork)) =>
        val maybeResult = SierraPhysicalDigitalWorkPair.mergeAndRedirectWork(
          physicalWork = physicalWork,
          digitalWork = digitalWork
        )
        maybeResult match {
          case Some(result) => {
            val remainingWorks = works.filterNot { w =>
              w.sourceIdentifier == physicalWork.sourceIdentifier ||
              w.sourceIdentifier == digitalWork.sourceIdentifier
            }
            remainingWorks ++ List(result.mergedWork, result.redirectedWork)
          }
          case None => works
        }
      case _ => works
    }
  }

  private def isSierraWork(work: UnidentifiedWork): Boolean =
    work.sourceIdentifier.identifierType == IdentifierType(
      "sierra-system-number")

  private def isDigitalWork(work: UnidentifiedWork): Boolean =
    work.workType match {
      case None    => false
      case Some(t) => t.id == "v" && t.label == "E-books"
    }

  private def isSierraDigitalWork(work: UnidentifiedWork): Boolean =
    isSierraWork(work) && isDigitalWork(work)

  private def isSierraPhysicalWork(work: UnidentifiedWork): Boolean =
    isSierraWork(work) && !isDigitalWork(work)
}
