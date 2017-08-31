package uk.ac.wellcome.platform.idminter.steps

import com.google.inject.Inject
import com.twitter.inject.{Logging, TwitterModuleFlags}
import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{SourceIdentifier, Work}
import uk.ac.wellcome.platform.idminter.database.IdentifiersDao
import uk.ac.wellcome.platform.idminter.model.Identifier
import uk.ac.wellcome.platform.idminter.utils.Identifiable

import scala.concurrent.Future
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.util.Try

class IdentifierGenerator @Inject()(identifiersDao: IdentifiersDao,
                                    metricsSender: MetricsSender)
    extends Logging
    with TwitterModuleFlags {

  def generateId(work: Work): Future[String] = {
    metricsSender.timeAndCount(
      "generate-id",
      () =>
        findMiroID(work) match {
          case Some(identifier) =>
            Future {
              retrieveOrGenerateCanonicalId(
                identifier,
                ontologyType = work.ontologyType
              ).get
            }
          case None =>
            error(s"Item $work did not contain a MiroID")
            Future.failed(new Exception(s"Item $work did not contain a MiroID"))
      }
    )
  }

  private def retrieveOrGenerateCanonicalId(
    identifier: SourceIdentifier,
    ontologyType: String): Try[String] = {
    identifiersDao.lookupID(List(identifier), ontologyType).flatMap {
      case Some(id) =>
        metricsSender.incrementCount("found-old-id")
        Try(id.CanonicalID)
      case None =>
        val result = generateAndSaveCanonicalId(identifier.value)

        metricsSender.incrementCount("generated-new-id")

        result
    }
  }

  private def findMiroID(work: Work): Option[SourceIdentifier] = {
    val maybeSourceIdentifier =
      work.identifiers.find(identifier =>
        identifier.identifierScheme == IdentifierSchemes.miroImageNumber)
    info(s"SourceIdentifier: $maybeSourceIdentifier")
    maybeSourceIdentifier
  }

  private def generateAndSaveCanonicalId(miroId: String): Try[String] = {
    val canonicalId = Identifiable.generate
    identifiersDao
      .saveIdentifier(Identifier(MiroID = miroId, CanonicalID = canonicalId))
      .map { _ =>
        canonicalId
      }
  }
}
