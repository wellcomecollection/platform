package uk.ac.wellcome.platform.matcher.workgraph

import scalax.collection.Graph
import scalax.collection.GraphPredef._
import uk.ac.wellcome.platform.matcher.models.{
  LinkedWorkUpdate,
  LinkedWorksGraph,
  WorkNode
}

import scala.collection.immutable.Iterable

object LinkedWorkGraphUpdater {
  def update(workUpdate: LinkedWorkUpdate,
             existingGraph: LinkedWorksGraph): LinkedWorksGraph = {

    val filteredLinkedWorks = existingGraphWithoutUpdatedNode(
      workUpdate.workId,
      existingGraph.linkedWorksSet)
    val edges = filteredLinkedWorks.flatMap(workNode => {
      toEdges(workNode.id, workNode.referencedWorkIds)
    }) ++ toEdges(workUpdate.workId, workUpdate.linkedIds)

    val nodes = existingGraph.linkedWorksSet.flatMap(workNode => {
      allNodes(workNode)
    }) + workUpdate.workId

    val g = Graph.from(edges = edges, nodes = nodes)

    def adjacentNodeIds(n: g.NodeT) = {
      n.diSuccessors.map(_.value).toList.sorted
    }

    LinkedWorksGraph(
      g.componentTraverser()
        .flatMap(component => {
          val nodeIds = component.nodes.map(_.value).toList
          val componentId = nodeIds.sorted.mkString("+")
          component.nodes.map(node => {
            WorkNode(
              id = node.value,
              referencedWorkIds = adjacentNodeIds(node),
              componentId = componentId
            )
          })
        })
        .toSet
    )
  }

  private def allNodes(workNode: WorkNode) = {
    workNode.id +: workNode.referencedWorkIds
  }

  private def toEdges(workId: String, linkedWorkIds: Iterable[String]) = {
    linkedWorkIds.map(workId ~> _)
  }

  private def existingGraphWithoutUpdatedNode(
    workId: String,
    workNodes: Set[WorkNode]) = {
    workNodes.filterNot(_.id == workId)
  }
}
