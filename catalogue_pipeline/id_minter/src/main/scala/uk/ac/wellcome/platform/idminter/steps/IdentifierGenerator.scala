package uk.ac.wellcome.platform.idminter.steps

import com.google.inject.Inject
import com.twitter.inject.{Logging, TwitterModuleFlags}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.SourceIdentifier
import uk.ac.wellcome.platform.idminter.database.IdentifiersDao
import uk.ac.wellcome.platform.idminter.models.Identifier
import uk.ac.wellcome.platform.idminter.utils.Identifiable

import scala.util.Try

class IdentifierGenerator @Inject()(
  identifiersDao: IdentifiersDao,
  metricsSender: MetricsSender
) extends Logging
    with TwitterModuleFlags {

  def retrieveOrGenerateCanonicalId(
    identifier: SourceIdentifier,
    ontologyType: String
  ): Try[String] = {
    Try {
      identifiersDao
        .lookupId(
          sourceIdentifier = identifier,
          ontologyType = ontologyType
        )
        .flatMap {
          case Some(id) =>
            metricsSender.incrementCount("found-old-id")
            Try(id.CanonicalId)
          case None =>
            val result =
              generateAndSaveCanonicalId(identifier, ontologyType)
            if (result.isSuccess)
              metricsSender.incrementCount("generated-new-id")

            result
        }
    }.flatten
  }

  private def generateAndSaveCanonicalId(
    identifier: SourceIdentifier,
    ontologyType: String
  ): Try[String] = {

    val canonicalId = Identifiable.generate
    identifiersDao
      .saveIdentifier(
        Identifier(
          CanonicalId = canonicalId,
          OntologyType = ontologyType,
          SourceSystem = identifier.identifierScheme.toString,
          SourceId = identifier.value
        ))
      .map { _ =>
        canonicalId
      }
  }
}
