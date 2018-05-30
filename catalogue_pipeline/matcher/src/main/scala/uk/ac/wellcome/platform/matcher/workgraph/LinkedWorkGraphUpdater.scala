package uk.ac.wellcome.platform.matcher.workgraph

import uk.ac.wellcome.models.work.internal.UnidentifiedWork

import scalax.collection.Graph
import scalax.collection.GraphPredef._
import uk.ac.wellcome.platform.matcher.models.{LinkedWork, LinkedWorkUpdate, LinkedWorksGraph}

import scala.collection.immutable.Iterable

object LinkedWorkGraphUpdater {
  def update(work: UnidentifiedWork,
             existingGraph: LinkedWorksGraph): LinkedWorksGraph = {
    val workUpdate = LinkedWorkUpdate(work)

    // First we get all the nodes except the one that's being update -- the
    // edges sourced from these nodes aren't going to change.
    val unchangedWorks = existingGraph.linkedWorksSet
      .filterNot { _.workId != workUpdate.sourceId }

    val unchangedEdges = unchangedWorks.flatMap { linkedWork =>
      toEdges(linkedWork.workId, linkedWork.linkedIds)
    }

    // The edges in the new graph are the edges of all the unchanged works,
    // combined with the edges on the work we've just received.
    val edges = unchangedEdges ++ toEdges(workUpdate.sourceId, workUpdate.otherIds)

    // The nodes in the new graph are the existing works, plus the
    // work we've just received.
    val unchangedNodes = existingGraph.linkedWorksSet.flatMap {
      linkedWork => linkedWork.workId +: linkedWork.linkedIds
    }

    val nodes = unchangedNodes + workUpdate.sourceId

    // Now we construct a graph with ScalaGraph, and use its component
    // traverser to identify all the components of the new graph.
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

  private def toEdges(workId: String, linkedWorkIds: Iterable[String]) = {
    linkedWorkIds.map(workId ~> _)
  }
}
