package uk.ac.wellcome.platform.merger.rules.singlepagemiro

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.logging.MergerLogging

/** If we have a Miro work and a Sierra work with a single item,
  * the Sierra work replaces the Miro work (because this is metadata
  * that we can update).
  *
  * Specifically:
  *
  *   - We copy across all the identifiers from the Miro work (except
  *     those which contain Sierra identifiers)
  *
  *   - We combine the locations on the items, and use the Miro iiif-image
  *     location for the thumbnail.
  *
  */
object SierraMiroMergeRule extends SierraMiroMerger with SierraMiroPartitioner {
  def mergeAndRedirectWorks(works: Seq[BaseWork]): Seq[BaseWork] = {
    partitionWorks(works)
      .map {
        case Partition(sierraWork, miroWork, otherWorks) =>
          val maybeResult = mergeAndRedirectWorkPair(
            sierraWork = sierraWork,
            miroWork = miroWork
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

trait SierraMiroMerger extends Logging with MergerLogging {
  def mergeAndRedirectWorkPair(
    sierraWork: UnidentifiedWork,
    miroWork: UnidentifiedWork): Option[List[BaseWork]] = {
    (sierraWork.items, miroWork.items) match {
      case (
          List(sierraItem: MaybeDisplayable[Item]),
          List(miroItem: Unidentifiable[Item])) =>
        info(s"Merging ${describeWorkPair(sierraWork, miroWork)}.")

        val mergedWork = sierraWork.copy(
          otherIdentifiers = mergeIdentifiers(sierraWork, miroWork),
          items = mergeItems(sierraItem, miroItem)
        )

        Some(
          List(
            mergedWork,
            UnidentifiedRedirectedWork(miroWork, sierraWork)
          ))
      case _ =>
        None
    }
  }

  private def mergeItems(sierraItem: MaybeDisplayable[Item],
                         miroItem: Unidentifiable[Item]) = {
    val sierraDigitalLocations = sierraItem.agent.locations.collect {
      case location: DigitalLocation => location
    }

    val items = sierraDigitalLocations match {
      case Nil =>
        val agent: Item = sierraItem.agent.copy(
          locations = sierraItem.agent.locations ++ miroItem.agent.locations
        )
        List(copyItem(sierraItem, agent))
      case _ =>
        debug(
          s"Sierra work already has digital location $sierraDigitalLocations takes precedence over Miro location.")
        List(sierraItem)
    }
    items
  }

  /**
    *  Exclude all Sierra identifiers from the Miro work when
    *  merging, not just the identifer to the Sierra merge target.
    *  This is because in some cases Miro works have incorrect Sierra
    *  identifiers and there is no way to edit them, so they are
    *  dropped here.
    */
  val doNotMergeIdentifierTypes =
    List("sierra-identifier", "sierra-system-number")
  private def mergeIdentifiers(sierraWork: UnidentifiedWork,
                               miroWork: UnidentifiedWork) = {
    sierraWork.otherIdentifiers ++
      miroWork.identifiers.filterNot(sourceIdentifier =>
        doNotMergeIdentifierTypes.contains(sourceIdentifier.identifierType.id))
  }

  /**
    * Need to wrap this to allow copying of an item for both Unidentifiable
    * and Identifiable types because MaybeDisplayable doesn't have a
    * copy method defined.
    */
  private def copyItem(item: MaybeDisplayable[Item], agent: Item) = {
    item match {
      case unidentifiable: Unidentifiable[_] =>
        unidentifiable.copy(agent = agent)
      case identifiable: Identifiable[_] => identifiable.copy(agent = agent)
    }
  }
}
