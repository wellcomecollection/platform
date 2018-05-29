package uk.ac.wellcome.platform.matcher.workgraph

import scalax.collection.Graph
import scalax.collection.GraphPredef._
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.matcher.WorkNode
import uk.ac.wellcome.platform.matcher.models.{WorkGraph, WorkUpdate}

import scala.collection.immutable.Iterable

object WorkGraphUpdater {
  def update(workUpdate: WorkUpdate,
             existingGraph: WorkGraph): WorkGraph = {
    val existingVersion = existingGraph.nodes.find(_.workId == workUpdate.workId) match {
      case Some(lw) => lw.version
      case None => 0
    }

    if (existingVersion >= workUpdate.version) {
      throw GracefulFailureException(new RuntimeException("Not processing old work update"))
    }

    val filteredLinkedWorks = existingGraphWithoutUpdatedNode(
      workUpdate.workId,
      existingGraph.nodes)

    val nodeVersions = filteredLinkedWorks.map { linkedWork =>
      (linkedWork.workId, linkedWork.version)
    }.toMap + (workUpdate.workId -> workUpdate.version)

    val edges = filteredLinkedWorks.flatMap(linkedWork => {
      toEdges(linkedWork.workId, linkedWork.linkedIds)
    }) ++ toEdges(workUpdate.workId, workUpdate.referencedWorkIds)

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
            WorkNode(node.value, nodeVersions.getOrElse(node.value, 0), adjacentNodeIds(node), componentIdentifier)
          })
        })
        .toSet
    )
  }

  private def allNodes(linkedWork: WorkNode) = {
    linkedWork.workId +: linkedWork.linkedIds
  }

  private def toEdges(workId: String, linkedWorkIds: Iterable[String]) = {
    linkedWorkIds.map(workId ~> _)
  }

  private def existingGraphWithoutUpdatedNode(
    workId: String,
    linkedWorksList: Set[WorkNode]) = {
    linkedWorksList.filterNot(_.workId == workId)
  }
}
