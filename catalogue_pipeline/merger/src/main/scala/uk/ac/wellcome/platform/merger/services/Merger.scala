package uk.ac.wellcome.platform.merger.services

import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.pairwise_transforms.{PairwiseResult, SierraPhysicalDigitalWorkPair}

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
  */
class Merger @Inject() (
  sierraPhysicalDigitalWorkPair: SierraPhysicalDigitalWorkPair
) extends Logging with MergerRules {
  def merge(works: Seq[UnidentifiedWork]): Seq[BaseWork] = {
    mergePhysicalDigitalPair(works)
      .getOrElse(works)
  }

  private def mergePhysicalDigitalPair(works: Seq[UnidentifiedWork]) = {
    if (works.size == 2) {
      val (digitalWorks, physicalWorks) = works.partition(isDigitalWork)
      mergePhysicalAndDigitalWorks(physicalWorks, digitalWorks)
    } else {
      None
    }
  }

  private def mergePhysicalAndDigitalWorks(
    physicalWorks: Seq[UnidentifiedWork],
    digitalWorks: Seq[UnidentifiedWork]) = {
    (physicalWorks, digitalWorks) match {
      // As the works are supplied by the matcher these are trusted to refer to the same work without verification.
      // However, it may be prudent to add extra checks before making the merge here.
      case (List(physicalWork), List(digitalWork)) =>
        mergeAndRedirectWork(physicalWork, digitalWork)
      case _ =>
        None
    }
  }

  private def mergeAndRedirectWork(physicalWork: UnidentifiedWork,
                                   digitalWork: UnidentifiedWork) = {
    val result = sierraPhysicalDigitalWorkPair.mergeAndRedirectWork(
      physicalWork = physicalWork,
      digitalWork = digitalWork
    )

    result.map { case PairwiseResult(mergedWork, redirectedWork) =>
      List(mergedWork, redirectedWork)
    }
  }

  private def isMiroWork(work: UnidentifiedWork): Boolean =
    work.sourceIdentifier.identifierType == IdentifierType("miro-image-number")

  private def isSierraWork(work: UnidentifiedWork): Boolean =
    work.sourceIdentifier.identifierType == IdentifierType("sierra-system-number")


  private def isDigitalWork(work: UnidentifiedWork): Boolean =
    work.workType match {
      case None    => false
      case Some(t) => t.id == "v" && t.label == "E-books"
    }

  private def isSierraDigitalWork(work: UnidentifiedWork): Boolean =
    isSierraWork(work) && isDigitalWork(work)
}
