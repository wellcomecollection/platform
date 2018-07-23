package uk.ac.wellcome.platform.merger.services

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.work.internal._

class Merger extends Logging {
  def merge(works: Seq[UnidentifiedWork]) = {
    mergePhysicalDigitalPair(works)
      .getOrElse(works)
  }

  private def mergePhysicalDigitalPair(works: Seq[UnidentifiedWork]) = {
    if(works.size == 2) {
      val (digitalWorks, physicalWorks) = works.partition(isDigitalWork)
      mergePhysicalAndDigitalWorks(physicalWorks, digitalWorks)
    } else {
      None
    }
  }

  private def mergePhysicalAndDigitalWorks(physicalWorks: Seq[UnidentifiedWork], digitalWorks: Seq[UnidentifiedWork]) = {
    if (physicalWorks.size == 1 && digitalWorks.size == 1) {
      val physicalWork = physicalWorks.head
      val digitalWork = digitalWorks.head
      Some(List(
        mergePhysicalWithDigitalWork(physicalWork, digitalWork),
        redirectWork(digitalWork, physicalWork.sourceIdentifier)))
    } else {
      None
    }
  }

  private def redirectWork(workToRedirect: UnidentifiedWork, redirectTargetSourceIdentifier: SourceIdentifier) = {
    UnidentifiedRedirectedWork(
      sourceIdentifier = workToRedirect.sourceIdentifier,
      version = workToRedirect.version,
      redirect = IdentifiableRedirect(redirectTargetSourceIdentifier)
    )
  }

  private def isDigitalWork(work: UnidentifiedWork): Boolean = {
    work.workType match {
      case None => false
      case Some(t) => t.id == "v" && t.label == "E-books"
    }
  }

  private def mergePhysicalWithDigitalWork(physicalWork: UnidentifiedWork, digitalWork: UnidentifiedWork) = {
    physicalWork.copy(items = physicalWork.items ++ digitalWork.items)
  }

}
