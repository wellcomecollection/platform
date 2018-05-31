package uk.ac.wellcome.platform.matcher.storage

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.matcher.models.{LinkedWorkUpdate, WorkGraph}
import uk.ac.wellcome.storage.GlobalExecutionContext._

import scala.concurrent.Future

class WorkGraphStore @Inject()(
  workNodeDao: WorkNodeDao
) extends Logging {

  def findAffectedWorks(workUpdate: LinkedWorkUpdate): Future[WorkGraph] = {
    val directlyAffectedWorkIds = workUpdate.linkedIds + workUpdate.workId

    for {
      directlyAffectedWorks <- workNodeDao.get(directlyAffectedWorkIds)
      affectedComponentIds = directlyAffectedWorks.map(workNode =>
        workNode.componentId)
      affectedWorks <- workNodeDao.getByComponentIds(affectedComponentIds)
    } yield WorkGraph(affectedWorks)
  }

  def put(graph: WorkGraph) = {
    Future.sequence(
      graph.nodes.map(workNodeDao.put)
    )
  }
}
