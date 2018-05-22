package uk.ac.wellcome.platform.matcher.matcher

import com.google.inject.Inject
import uk.ac.wellcome.models.work.internal.{SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.platform.matcher.models._
import uk.ac.wellcome.platform.matcher.storage.WorkGraphStore
import uk.ac.wellcome.platform.matcher.workgraph.LinkedWorkGraphUpdater
import uk.ac.wellcome.storage.GlobalExecutionContext._

import scala.concurrent.Future

class LinkedWorkMatcher @Inject()(workGraphStore: WorkGraphStore) {
  def matchWork(work: UnidentifiedWork) =
    matchLinkedWorks(work).map(LinkedWorksIdentifiersList)

  private def identifierToString(sourceIdentifier: SourceIdentifier): String =
    s"${sourceIdentifier.identifierScheme}/${sourceIdentifier.value}"

  private def matchLinkedWorks(work: UnidentifiedWork): Future[List[IdentifierList]] = {
    val workId = identifierToString(work.sourceIdentifier)
    val linkedWorkIds =
      work.identifiers.map(identifierToString).filterNot(_ == workId)

    // load from persisted graphs, assume no exisiting graph
    val eventualLinkedWorksGraph = workGraphStore.findAffectedWorks(LinkedWorkUpdate(workId, linkedWorkIds))

    eventualLinkedWorksGraph.map { linkedWorksGraph =>
      val updatedLinkedWorkGraph: LinkedWorksGraph =
        LinkedWorkGraphUpdater.update(
          LinkedWorkUpdate(workId, linkedWorkIds),
          linkedWorksGraph)

      // persist updated graph
      workGraphStore.put(updatedLinkedWorkGraph)

      // return just the ids in the groups
      updatedLinkedWorkGraph.linkedWorksSet
        .groupBy(_.setId)
        .map {
          case (setId, linkedWorkList) =>
            IdentifierList(linkedWorkList.map(_.workId))
        }
        .toList
    }
  }
}
