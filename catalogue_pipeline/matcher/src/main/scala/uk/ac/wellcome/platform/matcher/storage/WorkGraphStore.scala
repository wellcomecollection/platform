package uk.ac.wellcome.platform.matcher.storage

import com.google.inject.Inject
import com.gu.scanamo.error.DynamoReadError
import com.twitter.inject.Logging
import uk.ac.wellcome.models.matcher.WorkNode
import uk.ac.wellcome.platform.matcher.models.{WorkGraph, WorkUpdate}
import uk.ac.wellcome.storage.GlobalExecutionContext._

import scala.concurrent.Future

class WorkGraphStore @Inject()(
  workNodeDao: WorkNodeDao,
  dynamoLockingService: DynamoLockingService
) extends Logging {

  def findAffectedWorks(workUpdate: WorkUpdate): Future[WorkGraph] = {
    val directlyAffectedWorkIds = workUpdate.referencedWorkIds + workUpdate.workId

    val directlyAffectedWorks = workNodeDao.get(directlyAffectedWorkIds)
    val affectedComponentIds = directlyAffectedWorks.map(workNode => workNode.componentId)
    val affectedWorks = workNodeDao.getByComponentIds(affectedComponentIds)

    WorkGraph(affectedWorks)
  }

  def put(graph: WorkGraph)
    : Future[Set[Option[Either[DynamoReadError, WorkNode]]]] = {
    Future.sequence(
      graph.nodes.map(workNodeDao.put)
    )
  }
}
