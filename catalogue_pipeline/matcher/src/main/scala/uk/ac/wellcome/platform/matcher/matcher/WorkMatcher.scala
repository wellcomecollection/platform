package uk.ac.wellcome.platform.matcher.matcher

import com.google.inject.Inject
import uk.ac.wellcome.models.matcher.{MatchedIdentifiers, MatcherResult, WorkIdentifier, WorkNode}
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.platform.matcher.models._
import uk.ac.wellcome.platform.matcher.storage.WorkGraphStore
import uk.ac.wellcome.platform.matcher.workgraph.WorkGraphUpdater
import uk.ac.wellcome.storage.GlobalExecutionContext._

import scala.concurrent.Future

class WorkMatcher @Inject()(workGraphStore: WorkGraphStore) {
  def matchWork(work: UnidentifiedWork) =
    matchLinkedWorks(work).map(MatcherResult)

  type FutureMatched = Future[Set[MatchedIdentifiers]]

  private def matchLinkedWorks(work: UnidentifiedWork): FutureMatched = {
    val update = WorkUpdate(work)

    for {
      graph <- workGraphStore.findAffectedWorks(update)
      updatedGraph = WorkGraphUpdater.update(update, graph)
      _ <- workGraphStore.put(updatedGraph)

    } yield {
      convertToIdentifiersList(updatedGraph)
    }
  }

  private def convertToIdentifiersList(graph: WorkGraph) = {
    groupBySetId(graph).map {
      case (_, workNodes: Set[WorkNode]) =>
        MatchedIdentifiers(workNodes.map(WorkIdentifier(_)))
    }.toSet
  }

  private def groupBySetId(updatedGraph: WorkGraph) =
    updatedGraph.nodes.groupBy(_.componentId)

}
