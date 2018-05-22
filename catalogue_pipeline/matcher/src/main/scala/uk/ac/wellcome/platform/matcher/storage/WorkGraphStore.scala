package uk.ac.wellcome.platform.matcher.storage

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.matcher.models.{LinkedWork, LinkedWorkUpdate, LinkedWorksGraph}
import uk.ac.wellcome.storage.GlobalExecutionContext._

import scala.concurrent.Future

class WorkGraphStore @Inject()(
                                linkedWorkDao: LinkedWorkDao
                              ) extends Logging {

  def allLinkedWorks(workUpdate: LinkedWorkUpdate): Future[LinkedWorksGraph] = {

    val eventualMaybeWorkToUpdate: Future[List[LinkedWork]] = linkedWorkDao.get(workUpdate.workId).map(_.toList)
    val eventualMaybeLinkedWorksToUpdate: Future[List[LinkedWork]] = Future.sequence(workUpdate.linkedIds.map(linkedWorkDao.get)).map(_.flatten)

    val worksTodUpdate: Future[Set[LinkedWork]] = Future.sequence(List(eventualMaybeWorkToUpdate, eventualMaybeLinkedWorksToUpdate)).map(_.flatten.toSet)

    val eventualSetIds: Future[Set[String]] =
      worksTodUpdate.map(maybeLinkedWork =>
        maybeLinkedWork.map( linkedWork =>
          linkedWork.setId
        )
      )

    val eventualLinkedWorks: Future[Set[LinkedWork]] = eventualSetIds.flatMap { setIds => Future.sequence(setIds.map(linkedWorkDao.getBySetId)) }.map(_.flatten)
    eventualLinkedWorks.map(linkedWorks => LinkedWorksGraph(linkedWorks))
  }
}
