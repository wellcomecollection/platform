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
        mergeAndRedirectWork(physicalWork, digitalWork)
      case _ =>
        None
    }
  }

  private def mergeAndRedirectWork(physicalWork: UnidentifiedWork,
                                   digitalWork: UnidentifiedWork) = {
    if (physicalWork.items.size == 1 && digitalWork.items.size == 1) {
      info(s"Merging ${describeWorkPair(physicalWork, digitalWork)} work pair.")
      Some(
        List(
          mergePhysicalAndDigitalWorkItems(physicalWork, digitalWork),
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

  private def mergePhysicalAndDigitalWorkItems(
    physicalWork: UnidentifiedWork,
    digitalWork: UnidentifiedWork) = {
    val mergedItem = (physicalWork.items, digitalWork.items) match {
      case (List(physicalItem), List(digitalItem)) =>
        physicalItem.copy(agent = physicalItem.agent.copy(
          locations = physicalItem.agent.locations ++ digitalItem.agent.locations))
      case _ =>
        throw new IllegalArgumentException(
          s"Cannot merge, physical and digital works must both have a single item (physicalWork:${physicalWork.sourceIdentifier}, digitalWork:${digitalWork.sourceIdentifier}")
    }
    physicalWork.copy(items = List(mergedItem))
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

  private def describeWorkPair(physicalWork: UnidentifiedWork,
                               digitalWork: UnidentifiedWork) =
    s"physical (id=${physicalWork.sourceIdentifier.value}) and digital (id=${digitalWork.sourceIdentifier.value})"

  private def describeWorkPairWithItems(physicalWork: UnidentifiedWork,
                                        digitalWork: UnidentifiedWork) =
    s"physical (id=${physicalWork.sourceIdentifier.value}, itemsCount=${physicalWork.items.size}) and " +
      s"digital (id=${digitalWork.sourceIdentifier.value}, itemsCount=${digitalWork.items.size}) work"

}
