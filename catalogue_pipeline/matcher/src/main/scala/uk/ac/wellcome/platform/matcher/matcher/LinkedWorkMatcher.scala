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
    s"${sourceIdentifier.identifierType.id}/${sourceIdentifier.value}"

  private def matchLinkedWorks(
    work: UnidentifiedWork): Future[Set[IdentifierList]] = {
    val workId = identifierToString(work.sourceIdentifier)
    val linkedWorkIds =
      work.identifiers.map(identifierToString).filterNot(_ == workId).toSet

    for {
      existingGraph <- workGraphStore.findAffectedWorks(
        LinkedWorkUpdate(workId, linkedWorkIds))
      updatedGraph = LinkedWorkGraphUpdater.update(
        LinkedWorkUpdate(workId, linkedWorkIds),
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
