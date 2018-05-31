package uk.ac.wellcome.platform.matcher.matcher

import com.google.inject.Inject
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.platform.matcher.models._
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

  private def convertToIdentifiersList(workGraph: WorkGraph): Set[IdentifierList] =
    workGraph.nodes
      .groupBy { _.componentId }
      .values
      .map { (nodeSet: Set[WorkNode]) => nodeSet.map { _.id }}
      .map { (nodeIds: Set[String]) => IdentifierList(nodeIds) }
      .toSet
}
