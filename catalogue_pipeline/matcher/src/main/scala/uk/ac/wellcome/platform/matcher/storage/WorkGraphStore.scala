package uk.ac.wellcome.platform.matcher.storage

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.matcher.models.{WorkGraph, WorkNodeUpdate}
import uk.ac.wellcome.storage.GlobalExecutionContext._

import scala.concurrent.Future

class WorkGraphStore @Inject()(
  linkedWorkDao: LinkedWorkDao
) extends Logging {

  def findAffectedWorks(workNodeUpdate: WorkNodeUpdate): Future[WorkGraph] = {

    val directlyAffectedWorkIds = workNodeUpdate.referencedWorkIds + workNodeUpdate.id

    for {
      directlyAffectedWorks <- linkedWorkDao.get(directlyAffectedWorkIds)
      affectedSetIds = directlyAffectedWorks.map { workNode =>
        workNode.componentId
      }
      affectedWorks <- linkedWorkDao.getBySetIds(affectedSetIds)
    } yield WorkGraph(affectedWorks)
  }

  def put(graph: WorkGraph) = {
    Future.sequence(
      graph.nodes.map(linkedWorkDao.put)
    )
  }
}
