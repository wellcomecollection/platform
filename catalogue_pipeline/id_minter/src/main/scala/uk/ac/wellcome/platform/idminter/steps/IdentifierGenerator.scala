package uk.ac.wellcome.platform.idminter.steps

import com.google.inject.Inject
import com.twitter.inject.{Logging, TwitterModuleFlags}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.platform.idminter.database.IdentifiersDao
import uk.ac.wellcome.platform.idminter.models.Identifier
import uk.ac.wellcome.platform.idminter.utils.Identifiable
import uk.ac.wellcome.work_model.SourceIdentifier

import scala.util.Try

class IdentifierGenerator @Inject()(
  identifiersDao: IdentifiersDao,
  metricsSender: MetricsSender
) extends Logging
    with TwitterModuleFlags {

  def retrieveOrGenerateCanonicalId(
    identifier: SourceIdentifier
  ): Try[String] = {
    Try {
      identifiersDao
        .lookupId(
          sourceIdentifier = identifier
        )
        .flatMap {
          case Some(id) =>
            metricsSender.incrementCount("found-old-id")
            Try(id.CanonicalId)
          case None =>
            val result =
              generateAndSaveCanonicalId(identifier)
            if (result.isSuccess)
              metricsSender.incrementCount("generated-new-id")

            result
        }
    }.flatten
  }

  private def generateAndSaveCanonicalId(
    identifier: SourceIdentifier
  ): Try[String] = {

    val canonicalId = Identifiable.generate
    identifiersDao
      .saveIdentifier(
        Identifier(
          CanonicalId = canonicalId,
          OntologyType = identifier.ontologyType,
          SourceSystem = identifier.identifierScheme.toString,
          SourceId = identifier.value
        ))
      .map { _ =>
        canonicalId
      }
  }
}
