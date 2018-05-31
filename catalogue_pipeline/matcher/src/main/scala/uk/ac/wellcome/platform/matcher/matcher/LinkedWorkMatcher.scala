package uk.ac.wellcome.platform.matcher.matcher

import com.google.inject.Inject
import uk.ac.wellcome.models.matcher.{EquivalentIdentifiers, MatchResult}
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.platform.matcher.models._
import uk.ac.wellcome.platform.matcher.storage.WorkGraphStore
import uk.ac.wellcome.platform.matcher.workgraph.LinkedWorkGraphUpdater
import uk.ac.wellcome.storage.GlobalExecutionContext._

import scala.concurrent.Future

class LinkedWorkMatcher @Inject()(workGraphStore: WorkGraphStore) {
  def matchWork(work: UnidentifiedWork): Future[MatchResult] =
    matchLinkedWorks(work).map { MatchResult(_) }

  private def matchLinkedWorks(
    work: UnidentifiedWork): Future[Set[EquivalentIdentifiers]] = {
    val workNodeUpdate = WorkNodeUpdate(work)

    for {
      existingGraph <- workGraphStore.findAffectedWorks(workNodeUpdate)
      updatedGraph = LinkedWorkGraphUpdater.update(
        workNodeUpdate = workNodeUpdate,
        existingGraph = existingGraph
      )
      _ <- workGraphStore.put(updatedGraph)
    } yield {
      findEquivalentIdentifierSets(updatedGraph)
    }
  }

  private def findEquivalentIdentifierSets(workGraph: WorkGraph): Set[EquivalentIdentifiers] =
    workGraph.nodes
      .groupBy { _.componentId }
      .values
      .map { (nodeSet: Set[WorkNode]) => nodeSet.map { _.id }}
      .map { (nodeIds: Set[String]) => EquivalentIdentifiers(nodeIds) }
      .toSet
}
