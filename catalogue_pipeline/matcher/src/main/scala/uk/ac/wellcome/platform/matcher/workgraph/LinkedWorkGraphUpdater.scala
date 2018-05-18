package uk.ac.wellcome.platform.matcher.workgraph

import scalax.collection.Graph
import scalax.collection.GraphPredef._
import uk.ac.wellcome.platform.matcher.models.{LinkedWork, LinkedWorksGraph}

object LinkedWorkGraphUpdater {
  def update(workUpdate: LinkedWork,
             existingGraph: LinkedWorksGraph): LinkedWorksGraph = {

    val filteredLinkedWorks = existingGraphWithoutUpdatedNode(
      workUpdate.workId,
      existingGraph.linkedWorksList)
    val edges = filteredLinkedWorks.flatMap(linkedWork => {
      toEdges(linkedWork)
    }) ++ toEdges(workUpdate)

    val nodes = existingGraph.linkedWorksList.flatMap(linkedWork => {
      allNodes(linkedWork)
    }) :+ workUpdate.workId

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
        .toList
    )
  }

  private def allNodes(linkedWork: LinkedWork) = {
    linkedWork.workId +: linkedWork.linkedIds
  }

  private def toEdges(linkedWork: LinkedWork) = {
    linkedWork.linkedIds.map(linkedWork.workId ~> _)
  }

  private def existingGraphWithoutUpdatedNode(
    workId: String,
    linkedWorksList: List[LinkedWork]) = {
    linkedWorksList.filterNot(_.workId == workId)
  }
}
