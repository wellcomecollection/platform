package uk.ac.wellcome.platform.matcher.storage

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.matcher.WorkNode
import uk.ac.wellcome.platform.matcher.lockable.{DynamoLockingService, Identifier}
import uk.ac.wellcome.platform.matcher.models.{WorkGraph, WorkUpdate}
import uk.ac.wellcome.storage.GlobalExecutionContext._

import scala.concurrent.Future

class WorkGraphStore @Inject()(
  workNodeDao: WorkNodeDao,
  dynamoLockingService: DynamoLockingService
) extends Logging {

  implicit val lockingService: DynamoLockingService = dynamoLockingService

  def findAffectedWorks(workUpdate: WorkUpdate): Future[WorkGraph] = Future {
    val directlyAffectedWorkIds = workUpdate.referencedWorkIds + workUpdate.workId

    val directlyAffectedWorks = workNodeDao.get(directlyAffectedWorkIds)
    val affectedComponentIds = directlyAffectedWorks.map(workNode => workNode.componentId)
    val affectedWorks = workNodeDao.getByComponentIds(affectedComponentIds)

    WorkGraph(affectedWorks)
  }

  def put(graphBeforeUpdate: WorkGraph, updatedGraph: WorkGraph) = {
    val storedGraphIdentifiers = graphBeforeUpdate.nodes.map(node => Identifier(node.id))
    debug(s"Locking graph identifiers $storedGraphIdentifiers")
    storedGraphIdentifiers.foreach(lockingService.lockRow)

    val currentLockedGraphNodes: Set[WorkNode] = workNodeDao.get(storedGraphIdentifiers.map(_.id))

    if (currentLockedGraphNodes == graphBeforeUpdate.nodes) {
      updatedGraph.nodes.map(workNodeDao.put)
      debug(s"Successfully updated graph -- releasing graph identifier locks $storedGraphIdentifiers")
      storedGraphIdentifiers.foreach(lockingService.unlockRow)
    } else {
      info(s"Failed to update, graph has changed -- releasing graph identifier locks $storedGraphIdentifiers")
      storedGraphIdentifiers.foreach(lockingService.unlockRow)
      throw GracefulFailureException(new RuntimeException("Failed to lock graph for update"))
    }
  }
}
