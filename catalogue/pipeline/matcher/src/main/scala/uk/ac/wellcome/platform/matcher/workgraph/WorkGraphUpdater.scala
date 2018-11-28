package uk.ac.wellcome.platform.matcher.workgraph

import grizzled.slf4j.Logging
import org.apache.commons.codec.digest.DigestUtils
import scalax.collection.Graph
import scalax.collection.GraphPredef._
import uk.ac.wellcome.models.matcher.WorkNode
import uk.ac.wellcome.platform.matcher.models.{
  VersionExpectedConflictException,
  VersionUnexpectedConflictException,
  WorkGraph,
  WorkUpdate
}

import scala.collection.immutable.Iterable

object WorkGraphUpdater extends Logging {
  def update(workUpdate: WorkUpdate, existingGraph: WorkGraph): WorkGraph = {

    val maybeExistingNode =
      existingGraph.nodes.find(_.id == workUpdate.workId)

    maybeExistingNode match {
      case Some(WorkNode(_, existingVersion, _, _))
          if existingVersion > workUpdate.version =>
        val versionConflictMessage =
          s"update failed, work:${workUpdate.workId} v${workUpdate.version} is not newer than existing work v$existingVersion"
        debug(versionConflictMessage)
        throw VersionExpectedConflictException(versionConflictMessage)
      case Some(WorkNode(_, existingVersion, linkedIds, _))
          if existingVersion == workUpdate.version && workUpdate.referencedWorkIds != linkedIds.toSet =>
        val versionConflictMessage =
          s"update failed, work:${workUpdate.workId} v${workUpdate.version} already exists with different content! update-ids:${workUpdate.referencedWorkIds} != existing-ids:${linkedIds.toSet}"
        debug(versionConflictMessage)
        throw VersionUnexpectedConflictException(versionConflictMessage)
      case _ => doUpdate(workUpdate, existingGraph)
    }
  }

  private def doUpdate(workUpdate: WorkUpdate, existingGraph: WorkGraph) = {
    val filteredLinkedWorks =
      existingGraphWithoutUpdatedNode(workUpdate.workId, existingGraph.nodes)

    val nodeVersions = filteredLinkedWorks.map { linkedWork =>
      (linkedWork.id, linkedWork.version)
    }.toMap + (workUpdate.workId -> workUpdate.version)

    val edges = filteredLinkedWorks.flatMap(linkedWork => {
      toEdges(linkedWork.id, linkedWork.linkedIds)
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
          component.nodes.map(node => {
            WorkNode(
              node.value,
              nodeVersions.getOrElse(node.value, 0),
              adjacentNodeIds(node),
              componentIdentifier(nodeIds))
          })
        })
        .toSet
    )
  }

  private def componentIdentifier(nodeIds: List[String]) = {
    DigestUtils.sha256Hex(nodeIds.sorted.mkString("+"))
  }

  private def allNodes(linkedWork: WorkNode) = {
    linkedWork.id +: linkedWork.linkedIds
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
