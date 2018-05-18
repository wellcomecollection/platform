package uk.ac.wellcome.platform.matcher

import uk.ac.wellcome.models.work.internal.{SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.platform.matcher.workgraph.LinkedWorkGraphUpdater
import uk.ac.wellcome.platform.matcher.models.{IdentifierList, LinkedWork, LinkedWorksGraph, LinkedWorksList}

class LinkedWorkMatcher {
  def matchWork(work: UnidentifiedWork) =
    LinkedWorksList(matchLinkedWorks(work))

  private def identifierToString(sourceIdentifier: SourceIdentifier): String =
    s"${sourceIdentifier.identifierScheme}/${sourceIdentifier.value}"

  private def matchLinkedWorks(work: UnidentifiedWork) : List[IdentifierList] = {
    val workId = identifierToString(work.sourceIdentifier)
    val linkedWorkIds = work.identifiers.map(identifierToString).filterNot(_ == workId)

    // load from persisted graphs, assume no exisiting graph
    val existingLinkedWorkGraph = LinkedWorksGraph(List())

    val updatedLinkedWorkGraph =
      LinkedWorkGraphUpdater.update(
        LinkedWork(workId, linkedWorkIds),
        existingLinkedWorkGraph)

    // return just the ids in the groups
    updatedLinkedWorkGraph.linkedWorksList.map(linkedWork => {
      IdentifierList(linkedWork.workId :: linkedWork.linkedIds)
    })
  }
}
