package uk.ac.wellcome.platform.matcher.workgraph

import scalax.collection.Graph
import scalax.collection.GraphPredef._
import uk.ac.wellcome.platform.matcher.models.{
  WorkGraph,
  WorkNode,
  WorkNodeUpdate
}

import scala.collection.immutable.Iterable

object LinkedWorkGraphUpdater {

  // Given an update to an individual node, and the existing graph,
  // return the graph that comes from applying this update.
  def update(workNodeUpdate: WorkNodeUpdate,
             existingGraph: WorkGraph): WorkGraph = {

    // First we get every node except the updated node -- the edges coming
    // from these nodes won't be changing.
    val unchangedNodes: Set[WorkNode] = existingGraph.nodes
      .filterNot { _.id == workNodeUpdate.id }

    val unchangedEdges = unchangedNodes
      .flatMap { node =>
        toEdges(node.id, node.referencedWorkIds)
      }

    // Then we add the edges from the updated node.
    val updatedEdges =
      toEdges(workNodeUpdate.id, workNodeUpdate.referencedWorkIds)
    val edges = unchangedEdges ++ updatedEdges

    // And we get all the IDs from the existing graph, plus anything new.
    val nodeIds = existingGraph.nodes
      .flatMap { node =>
        allNodes(node)
      } + workNodeUpdate.id

    // Now we construct a graph with ScalaGraph, iterate over the connected
    // components, and extract the nodes from each component.
    val g = Graph.from(edges = edges, nodes = nodeIds)

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

  private def allNodes(node: WorkNode) =
    node.id +: node.referencedWorkIds

  private def toEdges(workId: String, referencedWorkIds: Iterable[String]) =
    referencedWorkIds.map(workId ~> _)
}
