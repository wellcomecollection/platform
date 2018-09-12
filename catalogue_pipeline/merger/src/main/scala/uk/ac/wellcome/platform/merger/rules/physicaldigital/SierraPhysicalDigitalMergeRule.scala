package uk.ac.wellcome.platform.merger.rules.physicaldigital

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.logging.MergerLogging

/** If we have a pair of Sierra records:
  *
  *   - One of which has a single Physical location ("physical work")
  *   - The other of which has a single Digital location ("digital work")
  *
  * Then the digital work is a digitised version of the physical work.
  * The physical work takes precedence, we copy the location from the digital
  * work, and redirect the digital work to the physical work.
  *
  */
object SierraPhysicalDigitalMergeRule
    extends Logging
    with SierraPhysicalDigitalMerger
    with SierraPhysicalDigitalPartitioner {

  def mergeAndRedirectWorks(works: Seq[UnidentifiedWork]): Seq[BaseWork] = {
    partitionWorks(works)
      .map {
        case Partition(physicalWork, digitalWork, otherWorks) =>
          val maybeResult = mergeAndRedirectWorkPair(
            physicalWork = physicalWork,
            digitalWork = digitalWork
          )
          maybeResult match {
            case Some(result) =>
              result ++ otherWorks
            case _ => works
          }
      }
      .getOrElse(works)
  }
}

trait SierraPhysicalDigitalMerger extends Logging with MergerLogging {
  def mergeAndRedirectWorkPair(
    physicalWork: UnidentifiedWork,
    digitalWork: UnidentifiedWork): Option[List[BaseWork]] =
    (physicalWork.items, digitalWork.items) match {
      case (
          List(physicalItem: Identifiable[Item]),
          List(digitalItem: Unidentifiable[Item])) =>
        info(
          s"Merging ${describeWorkPair(physicalWork, digitalWork)} work pair.")

        // Copy the identifiers and locations from the physical work on to
        // the digital work.  We know both works only have a single item,
        // so these locations definitely correspond to this item.
        val mergedWork = physicalWork.copy(
          otherIdentifiers = physicalWork.otherIdentifiers ++ digitalWork.identifiers,
          items = mergeItems(physicalItem, digitalItem)
        )

        Some(
          List(
            mergedWork,
            UnidentifiedRedirectedWork(digitalWork, physicalWork)
          ))
      case _ =>
        debug(
          s"Not merging physical ${describeWorkPairWithItems(physicalWork, digitalWork)} due to item counts.")
        None
    }

  private def mergeItems(physicalItem: Identifiable[Item],
                         digitalItem: Unidentifiable[Item]) = {
    List(
      physicalItem.copy(
        agent = physicalItem.agent.copy(
          locations = physicalItem.agent.locations ++ digitalItem.agent.locations
        )
      )
    )
  }
}
