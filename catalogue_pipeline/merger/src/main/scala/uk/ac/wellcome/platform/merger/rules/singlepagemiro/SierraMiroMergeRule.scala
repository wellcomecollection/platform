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
  *   - We copy across the Miro location to the Sierra work *if* the
  *     Sierra work doesn't already have a digital location.
  *     In this case, the Sierra digital location supersedes the one
  *     from Miro.
  *
  */
object SierraMiroMergeRule extends SierraMiroMerger with SierraMiroPartitioner {
  def mergeAndRedirectWork(works: Seq[BaseWork]): Seq[BaseWork] = {
    partitionWorks(works).map {
      case Partition(sierraWork, miroWork, otherWorks) =>
        val maybeResult = mergeAndRedirectWork(
          sierraWork = sierraWork,
          miroWork = miroWork
        )
        maybeResult match {
          case Some(result) =>
            result ++ otherWorks
          case _ => works
        }
    }.getOrElse(works)
  }
}

trait SierraMiroMerger
  extends Logging
    with MergerLogging {
  def mergeAndRedirectWork(sierraWork: UnidentifiedWork,
                           miroWork: UnidentifiedWork): Option[List[BaseWork]] = {
    (sierraWork.items, miroWork.items) match {
      case (List(sierraItem: MaybeDisplayable[Item]), List(miroItem: Unidentifiable[Item])) => {

        info(s"Merging ${describeWorkPair(sierraWork, miroWork)}.")

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
            debug(s"Sierra work already has digital location $sierraDigitalLocations takes precedence over Miro location.")
            List(sierraItem)
        }

        val mergedWork = sierraWork.copy(
          otherIdentifiers = sierraWork.otherIdentifiers ++ miroWork.identifiers.filterNot(identifier => identifier == sierraWork.sourceIdentifier),
          items = items
        )

        val redirectedWork = UnidentifiedRedirectedWork(
          sourceIdentifier = miroWork.sourceIdentifier,
          version = miroWork.version,
          redirect = IdentifiableRedirect(sierraWork.sourceIdentifier)
        )

        Some(
          List(
            mergedWork,
            redirectedWork
          ))
      }
      case _ =>
        None
    }
  }

  /**
    * Need to wrap this to allow copying of an item for both Unidentifiable and Identifiable types
    * because MaybeDisplayable doesn't have a copy method defined.
    */
  private def copyItem(item: MaybeDisplayable[Item], agent: Item) = {
    item match {
      case unidentifiable: Unidentifiable[_] => unidentifiable.copy(agent = agent)
      case identifiable: Identifiable[_] => identifiable.copy(agent = agent)
    }
  }
}

