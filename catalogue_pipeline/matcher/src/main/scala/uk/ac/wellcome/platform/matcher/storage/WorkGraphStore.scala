package uk.ac.wellcome.platform.matcher.storage

import com.google.inject.Inject
import com.gu.scanamo.error.DynamoReadError
import com.twitter.inject.Logging
import uk.ac.wellcome.models.matcher.WorkNode
import uk.ac.wellcome.platform.matcher.models.{WorkGraph, WorkUpdate}
import uk.ac.wellcome.storage.GlobalExecutionContext._

import scala.concurrent.Future

class WorkGraphStore @Inject()(
  workNodeDao: WorkNodeDao
) extends Logging {

  def findAffectedWorks(workUpdate: WorkUpdate): Future[WorkGraph] = {
    val directlyAffectedWorkIds = workUpdate.referencedWorkIds + workUpdate.workId

    for {
      directlyAffectedWorks <- workNodeDao.get(directlyAffectedWorkIds)
      affectedComponentIds = directlyAffectedWorks.map(workNode =>
        workNode.componentId)
      affectedWorks <- workNodeDao.getByComponentIds(affectedComponentIds)
    } yield WorkGraph(affectedWorks)
  }

  def put(graph: WorkGraph): Future[Set[Option[Either[DynamoReadError, WorkNode]]]] = {
    Future.sequence(
      graph.nodes.map(workNodeDao.put)
    )
  }
}
