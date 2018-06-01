package uk.ac.wellcome.platform.matcher.matcher

import com.google.inject.Inject
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.platform.matcher.models._
import uk.ac.wellcome.platform.matcher.storage.WorkGraphStore
import uk.ac.wellcome.platform.matcher.workgraph.WorkGraphUpdater
import uk.ac.wellcome.storage.GlobalExecutionContext._

import scala.concurrent.Future

class LinkedWorkMatcher @Inject()(workGraphStore: WorkGraphStore) {
  def matchWork(work: UnidentifiedWork) =
    matchLinkedWorks(work).map(LinkedWorksIdentifiersList)

  private def matchLinkedWorks(
    work: UnidentifiedWork): Future[Set[IdentifierList]] = {
    val workNodeUpdate = WorkUpdate(work)

    for {
      existingGraph <- workGraphStore.findAffectedWorks(workNodeUpdate)
      updatedGraph = WorkGraphUpdater.update(workNodeUpdate,
        existingGraph = existingGraph)
      _ <- workGraphStore.put(updatedGraph)

    } yield {
      convertToIdentifiersList(updatedGraph)
    }
  }

  private def convertToIdentifiersList(updatedGraph: WorkGraph) = {
    groupBySetId(updatedGraph).map {
      case (_, workNodeList) =>
        IdentifierList(workNodeList.map(_.id))
    }.toSet
  }

  private def groupBySetId(updatedGraph: WorkGraph) = {
    updatedGraph.nodes
      .groupBy(_.componentId)
  }
}
