package uk.ac.wellcome.platform.matcher.storage

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.platform.matcher.models.{
  LinkedWorkUpdate,
  LinkedWorksGraph
}
import uk.ac.wellcome.storage.GlobalExecutionContext._

import scala.concurrent.Future

class WorkGraphStore @Inject()(
  linkedWorkDao: LinkedWorkDao
) extends Logging {

  // Given a Work received from the recorder, return a graph that contains
  // all the nodes that might be affected to a change to this Work.
  //
  // e.g. if a Work X has identifiers {A, B, C}, it returns a graph including
  // A, B, C, every work with some connection to them, and any other works
  // that were already connected to X.
  //
  def findExistingGraph(work: UnidentifiedWork): Future[LinkedWorksGraph] = {
    val workUpdate = LinkedWorkUpdate(work)
    val directlyAffectedWorkIds = workUpdate.otherIds + workUpdate.sourceId

    for {
      directlyAffectedWorks <- linkedWorkDao.get(
        workIds = directlyAffectedWorkIds)
      affectedSetIds = directlyAffectedWorks.map { linkedWork =>
        linkedWork.setId
      }
      affectedWorks <- linkedWorkDao.getBySetIds(affectedSetIds)
    } yield LinkedWorksGraph(affectedWorks)
  }

  def put(graph: LinkedWorksGraph) = {
    Future.sequence(
      graph.linkedWorksSet.map(linkedWorkDao.put)
    )
  }
}
