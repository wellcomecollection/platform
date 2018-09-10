package uk.ac.wellcome.platform.merger.rules
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
object SierraPhysicalDigitalMergeRule extends Logging {

  private object workType extends Enumeration {
    val SierraDigitalWork, SierraPhysicalWork, OtherWork = Value
  }

  def mergeAndRedirectWork(works: Seq[UnidentifiedWork]): Seq[BaseWork] = {
    val groupedWorks = works.groupBy {
      case work if isSierraPhysicalWork(work) => workType.SierraPhysicalWork
      case work if isSierraDigitalWork(work) => workType.SierraDigitalWork
      case _ => workType.OtherWork
    }

    val physicalWorks = groupedWorks.get(workType.SierraPhysicalWork).toList.flatten
    val digitalWorks = groupedWorks.get(workType.SierraDigitalWork).toList.flatten

    (physicalWorks, digitalWorks) match {
      case (List(physicalWork), List(digitalWork)) =>
        val maybeResult = mergeAndRedirectWork(
          physicalWork = physicalWork,
          digitalWork = digitalWork
        )
        maybeResult match {
          case Some(result) => result ++ groupedWorks.get(workType.OtherWork).toList.flatten
          case _ => works
        }
      case _ => works
    }
  }

  private def mergeAndRedirectWork(
    physicalWork: UnidentifiedWork,
    digitalWork: UnidentifiedWork): Option[List[BaseWork]] =
    (physicalWork.items, digitalWork.items) match {
      case (
          List(physicalItem: Identifiable[Item]),
          List(digitalItem: Unidentifiable[Item])) => {
        info(
          s"Merging ${describeWorkPair(physicalWork, digitalWork)} work pair.")

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

        Some(
          List(
            mergedWork,
            redirectedWork
          ))
      }
      case _ =>
        debug(
          s"Not merging physical ${describeWorkPairWithItems(physicalWork, digitalWork)}; item counts are wrong")
        None
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

    private def describeWorkPair(physicalWork: UnidentifiedWork,
                               digitalWork: UnidentifiedWork) =
    s"physical (id=${physicalWork.sourceIdentifier.value}) and digital (id=${digitalWork.sourceIdentifier.value})"

  private def describeWorkPairWithItems(physicalWork: UnidentifiedWork,
                                        digitalWork: UnidentifiedWork) =
    s"physical (id=${physicalWork.sourceIdentifier.value}, itemsCount=${physicalWork.items.size}) and " +
      s"digital (id=${digitalWork.sourceIdentifier.value}, itemsCount=${digitalWork.items.size}) work"
}
