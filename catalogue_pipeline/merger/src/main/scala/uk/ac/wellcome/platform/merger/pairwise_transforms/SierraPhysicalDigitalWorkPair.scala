package uk.ac.wellcome.platform.merger.pairwise_transforms
import grizzled.slf4j.Logging
import uk.ac.wellcome.models.work.internal._

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
class SierraPhysicalDigitalWorkPair extends PairwiseMerger with Logging {
  def mergeAndRedirectWork(physicalWork: UnidentifiedWork,
                           digitalWork: UnidentifiedWork): Option[PairwiseResult] =
    (physicalWork.items, digitalWork.items) match {
      case (List(physicalItem: Identifiable[Item]), List(digitalItem: Unidentifiable[Item])) => {
        info(s"Merging ${describeWorkPair(physicalWork, digitalWork)} work pair.")

        // Copy the identifiers and locations from the physical work on to
        // the digital work.  We know both works only have a single item,
        // so these locations definitely correspond to this item.
        val mergedWork = physicalWork.copy(
          otherIdentifiers = physicalWork.otherIdentifiers ++ digitalWork.identifiers,
          items = List(
            physicalItem.copy(
              agent = physicalItem.agent.copy(
                locations = physicalItem.agent.locations ++ digitalItem.agent.locations
              )
            )
          )
        )

        val redirectedWork = UnidentifiedRedirectedWork(
          sourceIdentifier = digitalWork.sourceIdentifier,
          version = digitalWork.version,
          redirect = IdentifiableRedirect(physicalWork.sourceIdentifier)
        )

        Some(PairwiseResult(
          mergedWork = mergedWork,
          redirectedWork = redirectedWork
        ))
      }
      case _ =>
        debug(
          s"Not merging physical ${describeWorkPairWithItems(physicalWork, digitalWork)}; item counts are wrong")
        None
    }

  private def describeWorkPair(physicalWork: UnidentifiedWork,
                               digitalWork: UnidentifiedWork) =
    s"physical (id=${physicalWork.sourceIdentifier.value}) and digital (id=${digitalWork.sourceIdentifier.value})"

  private def describeWorkPairWithItems(physicalWork: UnidentifiedWork,
                                        digitalWork: UnidentifiedWork) =
    s"physical (id=${physicalWork.sourceIdentifier.value}, itemsCount=${physicalWork.items.size}) and " +
      s"digital (id=${digitalWork.sourceIdentifier.value}, itemsCount=${digitalWork.items.size}) work"
}
