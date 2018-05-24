package uk.ac.wellcome.platform.matcher.workgraph

import scalax.collection.Graph
import scalax.collection.GraphPredef._
import uk.ac.wellcome.platform.matcher.models.{
  LinkedWork,
  LinkedWorkUpdate,
  LinkedWorksGraph
}

import scala.collection.immutable.Iterable

object LinkedWorkGraphUpdater {
  def update(workUpdate: LinkedWorkUpdate,
             existingGraph: LinkedWorksGraph): LinkedWorksGraph = {

    val filteredLinkedWorks = existingGraphWithoutUpdatedNode(
      workUpdate.workId,
      existingGraph.linkedWorksSet)
    val edges = filteredLinkedWorks.flatMap(linkedWork => {
      toEdges(linkedWork.workId, linkedWork.linkedIds)
    }) ++ toEdges(workUpdate.workId, workUpdate.linkedIds)

    val nodes = existingGraph.linkedWorksSet.flatMap(linkedWork => {
      allNodes(linkedWork)
    }) + workUpdate.workId

    val g = Graph.from(edges = edges, nodes = nodes)

    def adjacentNodeIds(n: g.NodeT) = {
      n.diSuccessors.map(_.value).toList.sorted
    }

    LinkedWorksGraph(
      g.componentTraverser()
        .flatMap(component => {
          val nodeIds = component.nodes.map(_.value).toList
          val componentIdentifier = nodeIds.sorted.mkString("+")
          component.nodes.map(node => {
            LinkedWork(node.value, adjacentNodeIds(node), componentIdentifier)
          })
        })
        .toSet
    )
  }

  private def allNodes(linkedWork: LinkedWork) = {
    linkedWork.workId +: linkedWork.linkedIds
  }

  private def toEdges(workId: String, linkedWorkIds: Iterable[String]) = {
    linkedWorkIds.map(workId ~> _)
  }

  private def existingGraphWithoutUpdatedNode(
    workId: String,
    linkedWorksList: Set[LinkedWork]) = {
    linkedWorksList.filterNot(_.workId == workId)
  }
}
