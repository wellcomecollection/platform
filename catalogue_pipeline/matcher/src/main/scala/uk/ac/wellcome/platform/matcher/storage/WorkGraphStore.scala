package uk.ac.wellcome.platform.matcher.storage

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.matcher.models.{LinkedWorkUpdate, LinkedWorksGraph}
import uk.ac.wellcome.storage.GlobalExecutionContext._

import scala.concurrent.Future

class WorkGraphStore @Inject()(
                                linkedWorkDao: LinkedWorkDao
                              ) extends Logging {


  def findAffectedWorks(workUpdate: LinkedWorkUpdate): Future[LinkedWorksGraph] = {

    val directlyAffectedWorkIds = workUpdate.workId +: workUpdate.linkedIds

    for {
      direcltyAffectedWorks <- linkedWorkDao.get(directlyAffectedWorkIds.toSet)
      affectedSetIds = direcltyAffectedWorks.map( linkedWork =>
        linkedWork.setId
      )
      affectedWorks <- linkedWorkDao.getBySetIds(affectedSetIds)
    }
      yield LinkedWorksGraph(affectedWorks)
  }

  def put(graph: LinkedWorksGraph) = {
    Future.sequence(
      graph.linkedWorksSet.map(linkedWorkDao.put)
    )
  }
}
