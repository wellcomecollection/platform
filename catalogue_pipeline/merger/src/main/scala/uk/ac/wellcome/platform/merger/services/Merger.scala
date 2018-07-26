package uk.ac.wellcome.platform.merger.services

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.work.internal._

class Merger extends Logging {
  def merge(works: Seq[UnidentifiedWork]) = {
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
        mergePhysicalWorkWithDigitalAndRedirectDigitalWork(
          physicalWork,
          digitalWork)
      case _ =>
        None
    }
  }

  private def mergePhysicalWorkWithDigitalAndRedirectDigitalWork(
    physicalWork: UnidentifiedWork,
    digitalWork: UnidentifiedWork) = {
    if (physicalWork.items.size == 1 && digitalWork.items.size == 1) {
      info(s"Merging ${describeWorkPair(physicalWork, digitalWork)} work pair.")
      Some(
        List(
          mergePhysicalWithDigitalWork(physicalWork, digitalWork),
          redirectWork(
            workToRedirect = digitalWork,
            redirectTargetSourceIdentifier = physicalWork.sourceIdentifier)
        ))
    } else {
      debug(
        s"Not merging physical ${describeWorkPairWithItems(physicalWork, digitalWork)} as there are multiple items")
      None
    }
  }

  private def mergePhysicalWithDigitalWork(physicalWork: UnidentifiedWork,
                                           digitalWork: UnidentifiedWork) = {
    physicalWork.copy(items = List(physicalWork.items.head.copy(
      agent = physicalWork.items.head.agent.copy(locations =
        physicalWork.items.head.agent.locations ++ digitalWork.items.head.agent.locations))))
  }

  private def redirectWork(workToRedirect: UnidentifiedWork,
                           redirectTargetSourceIdentifier: SourceIdentifier) = {
    UnidentifiedRedirectedWork(
      sourceIdentifier = workToRedirect.sourceIdentifier,
      version = workToRedirect.version,
      redirect = IdentifiableRedirect(redirectTargetSourceIdentifier)
    )
  }

  private def isDigitalWork(work: UnidentifiedWork): Boolean = {
    work.workType match {
      case None    => false
      case Some(t) => t.id == "v" && t.label == "E-books"
    }
  }

  def describeWorkPair(physicalWork: UnidentifiedWork,
                       digitalWork: UnidentifiedWork) =
    s"physical (id=${physicalWork.sourceIdentifier.value}) and digital (id=${digitalWork.sourceIdentifier.value})"

  def describeWorkPairWithItems(physicalWork: UnidentifiedWork,
                                digitalWork: UnidentifiedWork) =
    s"physical (id=${physicalWork.sourceIdentifier.value}, itemsCount=${physicalWork.items.size}) and " +
      s"digital (id=${digitalWork.sourceIdentifier.value}, itemsCount=${digitalWork.items.size}) work"

}
