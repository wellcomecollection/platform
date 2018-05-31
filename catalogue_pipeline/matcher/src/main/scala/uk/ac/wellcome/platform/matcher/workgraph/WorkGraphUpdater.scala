package uk.ac.wellcome.platform.matcher.workgraph

import scalax.collection.Graph
import scalax.collection.GraphPredef._
import uk.ac.wellcome.platform.matcher.models.{WorkGraph, WorkNode, WorkUpdate}

import scala.collection.immutable.Iterable

object WorkGraphUpdater {
  def update(workUpdate: WorkUpdate, existingGraph: WorkGraph): WorkGraph = {

    val filteredLinkedWorks =
      existingGraphWithoutUpdatedNode(workUpdate.id, existingGraph.nodes)
    val edges = filteredLinkedWorks.flatMap(workNode => {
      toEdges(workNode.id, workNode.referencedWorkIds)
    }) ++ toEdges(workUpdate.id, workUpdate.referencedWorkIds)

    val nodes = existingGraph.nodes.flatMap(workNode => {
      allNodes(workNode)
    }) + workUpdate.id

    val g = Graph.from(edges = edges, nodes = nodes)

    def adjacentNodeIds(n: g.NodeT) = {
      n.diSuccessors.map(_.value).toList.sorted
    }

    WorkGraph(
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

  private def existingGraphWithoutUpdatedNode(workId: String,
                                              workNodes: Set[WorkNode]) = {
    workNodes.filterNot(_.id == workId)
  }
}
