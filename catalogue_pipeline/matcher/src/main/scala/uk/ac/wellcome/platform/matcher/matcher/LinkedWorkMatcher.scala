package uk.ac.wellcome.platform.matcher.matcher

import com.google.inject.Inject
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.platform.matcher.models.{
  IdentifierList,
  LinkedWorksIdentifiersList,
  WorkGraph,
  WorkNodeUpdate
}
import uk.ac.wellcome.platform.matcher.storage.WorkGraphStore
import uk.ac.wellcome.platform.matcher.workgraph.LinkedWorkGraphUpdater
import uk.ac.wellcome.storage.GlobalExecutionContext._

import scala.concurrent.Future

class LinkedWorkMatcher @Inject()(workGraphStore: WorkGraphStore) {
  def matchWork(work: UnidentifiedWork) =
    matchLinkedWorks(work).map(LinkedWorksIdentifiersList)

  private def matchLinkedWorks(
    work: UnidentifiedWork): Future[Set[IdentifierList]] = {
    val workNodeUpdate = WorkNodeUpdate(work)

    for {
      linkedWorksGraph <- workGraphStore.findAffectedWorks(workNodeUpdate)
      updatedLinkedWorkGraph = LinkedWorkGraphUpdater.update(
        workNodeUpdate,
        linkedWorksGraph)
      _ <- workGraphStore.put(updatedLinkedWorkGraph)

    } yield {
      convertToIdentifiersList(updatedLinkedWorkGraph)
    }
  }

  private def convertToIdentifiersList(
    updatedLinkedWorkGraph: WorkGraph) = {
    groupBySetId(updatedLinkedWorkGraph).map {
      case (_, linkedWorkList) =>
        IdentifierList(linkedWorkList.map(_.id))
    }.toSet
  }

  private def groupBySetId(updatedLinkedWorkGraph: WorkGraph) = {
    updatedLinkedWorkGraph.nodes
      .groupBy(_.componentId)
  }
}
