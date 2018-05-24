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

  private def matchLinkedWorks(
    work: UnidentifiedWork): Future[Set[IdentifierList]] = {
    val workId = identifierToString(work.sourceIdentifier)
    val linkedWorkIds =
      work.identifiers.map(identifierToString).filterNot(_ == workId).toSet

    for {
      linkedWorksGraph <- workGraphStore.findAffectedWorks(
        LinkedWorkUpdate(workId, linkedWorkIds))
      updatedLinkedWorkGraph = LinkedWorkGraphUpdater.update(
        LinkedWorkUpdate(workId, linkedWorkIds),
        linkedWorksGraph)
      _ <- workGraphStore.put(updatedLinkedWorkGraph)

    } yield {
      convertToIdentifiersList(updatedLinkedWorkGraph)
    }
  }

  private def convertToIdentifiersList(
    updatedLinkedWorkGraph: LinkedWorksGraph) = {
    groupBySetId(updatedLinkedWorkGraph).map {
      case (_, linkedWorkList) =>
        IdentifierList(linkedWorkList.map(_.workId))
    }.toSet
  }

  private def groupBySetId(updatedLinkedWorkGraph: LinkedWorksGraph) = {
    updatedLinkedWorkGraph.linkedWorksSet
      .groupBy(_.setId)
  }
}
