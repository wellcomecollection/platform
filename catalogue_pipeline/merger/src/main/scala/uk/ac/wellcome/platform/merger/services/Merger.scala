package uk.ac.wellcome.platform.merger.services

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.pairwise_transforms.{PairwiseResult, SierraPhysicalDigitalWorkPair}

trait MergerRules {
  def merge(works: Seq[UnidentifiedWork]): Seq[BaseWork]
}

class Merger extends Logging with MergerRules {
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
    val result = SierraPhysicalDigitalWorkPair.mergeAndRedirectWork(
      physicalWork = physicalWork,
      digitalWork = digitalWork
    )

    result.map { case PairwiseResult(mergedWork, redirectedWork) =>
      List(mergedWork, redirectedWork)
    }
  }

  private def isDigitalWork(work: UnidentifiedWork): Boolean = {
    work.workType match {
      case None    => false
      case Some(t) => t.id == "v" && t.label == "E-books"
    }
  }
}
