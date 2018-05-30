package uk.ac.wellcome.platform.matcher.workgraph

import scalax.collection.Graph
import scalax.collection.GraphPredef._
import uk.ac.wellcome.platform.matcher.models.{
  LinkedWorkUpdate,
  WorkGraph,
  WorkNode
}

import scala.collection.immutable.Iterable

object LinkedWorkGraphUpdater {
  def update(workUpdate: LinkedWorkUpdate,
             existingGraph: WorkGraph): WorkGraph = {

    val filteredLinkedWorks = existingGraphWithoutUpdatedNode(
      workUpdate.workId,
      existingGraph.nodes)
    val edges = filteredLinkedWorks.flatMap(linkedWork => {
      toEdges(linkedWork.id, linkedWork.referencedWorkIds)
    }) ++ toEdges(workUpdate.workId, workUpdate.linkedIds)

    val nodes = existingGraph.nodes.flatMap(linkedWork => {
      allNodes(linkedWork)
    }) + workUpdate.workId

    val g = Graph.from(edges = edges, nodes = nodes)

    def adjacentNodeIds(n: g.NodeT) = {
      n.diSuccessors.map(_.value).toList.sorted
    }

    WorkGraph(
      g.componentTraverser()
        .flatMap(component => {
          val nodeIds = component.nodes.map(_.value).toList
          val componentIdentifier = nodeIds.sorted.mkString("+")
          component.nodes.map(node => {
            WorkNode(
              id = node.value,
              referencedWorkIds = adjacentNodeIds(node),
              componentId = componentIdentifier
            )
          })
        })
        .toSet
    )
  }

  private def allNodes(linkedWork: WorkNode) = {
    linkedWork.id +: linkedWork.referencedWorkIds
  }

  private def toEdges(workId: String, linkedWorkIds: Iterable[String]) = {
    linkedWorkIds.map(workId ~> _)
  }

  private def existingGraphWithoutUpdatedNode(
    workId: String,
    linkedWorksList: Set[WorkNode]) = {
    linkedWorksList.filterNot(_.id == workId)
  }
}
