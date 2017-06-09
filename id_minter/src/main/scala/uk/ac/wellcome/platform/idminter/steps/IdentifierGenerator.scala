package uk.ac.wellcome.platform.idminter.steps

import com.google.inject.Inject
import com.twitter.inject.{Logging, TwitterModuleFlags}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{SourceIdentifier, Work}
import uk.ac.wellcome.platform.idminter.database.IdentifiersDao
import uk.ac.wellcome.platform.idminter.model.Identifier
import uk.ac.wellcome.platform.idminter.utils.Identifiable

import scala.concurrent.Future
import uk.ac.wellcome.utils.GlobalExecutionContext.context

class IdentifierGenerator @Inject()(identifiersDao: IdentifiersDao,
                                    metricsSender: MetricsSender)

    extends Logging
    with TwitterModuleFlags {

  def generateId(work: Work): Future[String] = {
    metricsSender.timeAndCount("generate-id", () =>
      findMiroID(work) match {
        case Some(identifier) => retrieveOrGenerateCanonicalId(identifier)
        case None =>
          error(s"Item $work did not contain a MiroID")
          Future.failed(new Exception(s"Item $work did not contain a MiroID"))
      }
    )
  }

  private def retrieveOrGenerateCanonicalId(
    identifier: SourceIdentifier): Future[String] =
    identifiersDao.findSourceIdInDb(identifier.value).flatMap {
      case Some(id) => Future.successful(id.CanonicalID)
      case None => generateAndSaveCanonicalId(identifier.value)
    }

  private def findMiroID(work: Work) = {
    val maybeSourceIdentifier =
      work.identifiers.find(identifier => identifier.sourceId == "MiroID")
    info(s"SourceIdentifier: $maybeSourceIdentifier")
    maybeSourceIdentifier
  }

  private def generateAndSaveCanonicalId(miroId: String): Future[String] = {
    val canonicalId = Identifiable.generate
    identifiersDao
      .saveIdentifier(Identifier(MiroID = miroId, CanonicalID = canonicalId))
      .map { _ =>
        canonicalId
      }
  }
}
