package uk.ac.wellcome.platform.merger.rules.singlepagemiro

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.work.internal._

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

trait SierraMiroMerger extends Logging {
  def mergeAndRedirectWork(sierraWork: UnidentifiedWork,
                           miroWork: UnidentifiedWork): Option[List[BaseWork]] = {
    (sierraWork.items, miroWork.items) match {
      case (List(sierraItem: Identifiable[Item]), List(miroItem: Unidentifiable[Item])) => {

        val mergedWork = sierraWork.copy(
          otherIdentifiers = sierraWork.otherIdentifiers ++ miroWork.identifiers,
          items = List(
            sierraItem.copy(
              agent = sierraItem.agent.copy(
                locations = sierraItem.agent.locations ++ miroItem.agent.locations
              )
            )
          )
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
}

