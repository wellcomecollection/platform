package uk.ac.wellcome.platform.matcher.matcher

import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.matcher.{
  MatchedIdentifiers,
  MatcherResult,
  WorkIdentifier,
  WorkNode
}
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.platform.matcher.lockable.{
  DynamoLockingService,
  FailedLockException,
  FailedUnlockException
}
import uk.ac.wellcome.platform.matcher.models._
import uk.ac.wellcome.platform.matcher.storage.WorkGraphStore
import uk.ac.wellcome.platform.matcher.workgraph.WorkGraphUpdater

import scala.concurrent.{ExecutionContext, Future}

class WorkMatcher @Inject()(
  workGraphStore: WorkGraphStore,
  lockingService: DynamoLockingService)(implicit context: ExecutionContext)
    extends Logging {

  def matchWork(work: UnidentifiedWork): Future[MatcherResult] =
    doMatch(work).map(MatcherResult)

  type FutureMatched = Future[Set[MatchedIdentifiers]]

  private def doMatch(work: UnidentifiedWork): FutureMatched = {
    val update = WorkUpdate(work)

    val updateAffectedIdentifiers = update.referencedWorkIds + update.workId
    lockingService
      .withLocks(updateAffectedIdentifiers)(
        withUpdateLocked(update, updateAffectedIdentifiers)
      )
      .recover {
        case e @ (_: FailedLockException | _: FailedUnlockException) =>
          debug(
            s"Locking failed while matching work ${work.sourceIdentifier} ${e.getClass.getSimpleName} ${e.getMessage}")
          throw GracefulFailureException(e)
      }
  }

  private def withUpdateLocked(update: WorkUpdate,
                               updateAffectedIdentifiers: Set[String]) = {
    for {
      graphBeforeUpdate <- workGraphStore.findAffectedWorks(update)
      updatedGraph = WorkGraphUpdater.update(update, graphBeforeUpdate)
      _ <- lockingService.withLocks(
        graphBeforeUpdate.nodes.map(_.id) -- updateAffectedIdentifiers)(
        workGraphStore.put(updatedGraph)
      )
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
