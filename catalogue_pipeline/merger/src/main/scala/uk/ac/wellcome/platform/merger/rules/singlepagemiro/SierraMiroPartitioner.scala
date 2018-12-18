package uk.ac.wellcome.platform.merger.rules.singlepagemiro

import uk.ac.wellcome.models.work.internal.{
  BaseWork,
  IdentifierType,
  UnidentifiedWork
}
import uk.ac.wellcome.platform.merger.rules.{Partition, Partitioner}

trait SierraMiroPartitioner extends Partitioner {

  private object workType extends Enumeration {
    val SierraWork, MiroWork, OtherWork = Value
  }

  override def partitionWorks(works: Seq[BaseWork]): Option[Partition] = {
    val groupedWorks = works.groupBy {
      case work: UnidentifiedWork if isSierraWork(work) => workType.SierraWork
      case work: UnidentifiedWork if isMiroWork(work)   => workType.MiroWork
      case _                                            => workType.OtherWork
    }

    val sierraWorks =
      groupedWorks.get(workType.SierraWork).toList.flatten
    val miroWorks =
      groupedWorks.get(workType.MiroWork).toList.flatten
    val otherWorks = groupedWorks.get(workType.OtherWork).toList.flatten

    (sierraWorks, miroWorks) match {
      case (
          List(sierraWork: UnidentifiedWork),
          List(miroWork: UnidentifiedWork)) =>
        Some(Partition(sierraWork, miroWork, otherWorks))
      case _ => None
    }
  }

  private def isSierraWork(work: UnidentifiedWork): Boolean =
    work.sourceIdentifier.identifierType ==
      IdentifierType("sierra-system-number")

  private def isMiroWork(work: UnidentifiedWork): Boolean =
    work.sourceIdentifier.identifierType ==
      IdentifierType("miro-image-number")
}
