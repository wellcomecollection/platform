package uk.ac.wellcome.platform.matcher.workgraph

import uk.ac.wellcome.platform.matcher.models.{LinkedWork, LinkedWorksGraph}

object LinkedWorkGraphUpdater {
  def update(workUpdate: LinkedWork, existingGraph: LinkedWorksGraph): LinkedWorksGraph = {
    LinkedWorksGraph(
      (List(LinkedWork(workUpdate.workId, workUpdate.linkedIds)) ++ existingGraph.linkedWorksList).distinct
    )
  }
}


//    val directlyAffectedExistingRedirects = existingRedirects.filter(
//      redirect => work.linkedIds.contains(redirect.workId))

//    val matchedWorkId =
//      combinedIdentifier(work.linkedIds, directlyAffectedExistingRedirects)
//
//    (redirectToCombined(existingRedirects, matchedWorkId) ++
//      newRedirects(work.linkedIds, matchedWorkId) :+
//      selfRedirect(matchedWorkId)).distinct
//  }

//  private def selfRedirect(combinedIdentifier: String) = {
//    LinkedWorkIds(workId = combinedIdentifier, identifiers = combinedIdentifier)
//  }
//
//  private def newRedirects(linkedIds: List[String],
//                           combinedIdentifier: String) = {
//    linkedIds.map(linkedId =>
//      LinkedWorkIds(workId = linkedId, identifiers = combinedIdentifier))
//  }
//
//  private def redirectToCombined(existingRedirects: List[LinkedWorkIds],
//                                 combinedIdentifier: String) = {
//    existingRedirects.map(_.copy(identifiers = combinedIdentifier))
//  }
//
//  private def combinedIdentifier(linkedIds: List[String],
//                                 directlyAffectedRedirects: List[LinkedWorkIds]) = {
//    (getParticipatingNodeIds(directlyAffectedRedirects) ++ linkedIds).distinct.sorted
//      .mkString("+")
//  }
//
//  private def getParticipatingNodeIds(
//    directlyAffectedRedirects: List[LinkedWorkIds]) = {
//    directlyAffectedRedirects.flatMap(_.identifiers.  split("\\+"))
//  }
//}