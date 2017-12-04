package uk.ac.wellcome.platform.idminter.steps

import com.google.inject.Inject
import com.twitter.inject.annotations.Flag
import com.twitter.inject.{Logging, TwitterModuleFlags}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier}
import uk.ac.wellcome.platform.idminter.database.{IdentifiersDao, UnableToMintIdentifierException}
import uk.ac.wellcome.platform.idminter.model.Identifier
import uk.ac.wellcome.platform.idminter.utils.Identifiable

import scala.util.Try

class IdentifierGenerator @Inject()(
  identifiersDao: IdentifiersDao,
  metricsSender: MetricsSender,
  @Flag("known.identifierSchemes") knownIdentifierSchemes: String)
    extends Logging
    with TwitterModuleFlags {
  private val knownIdentifierSchemeList =
    knownIdentifierSchemes.split(",").map(_.trim).toList

  def retrieveOrGenerateCanonicalId(identifiers: List[SourceIdentifier],
                                    ontologyType: String): Try[String] = {
    Try {
      val idsWithKnownSchemes = identifiers.filter(identifier =>
        knownIdentifierSchemeList.contains(identifier.identifierScheme))
      if (idsWithKnownSchemes.isEmpty) {
        throw UnableToMintIdentifierException(
          "identifiers list did not contain a known identifierScheme")
      } else {
        identifiersDao.lookupID(idsWithKnownSchemes, ontologyType).flatMap {
          case Some(id) =>
            metricsSender.incrementCount("found-old-id")
            Try(id.CanonicalID)
          case None =>
            val result =
              generateAndSaveCanonicalId(idsWithKnownSchemes, ontologyType)
            if (result.isSuccess)
              metricsSender.incrementCount("generated-new-id")

            result
        }
      }
    }.flatten
  }

  private def generateAndSaveCanonicalId(identifiers: List[SourceIdentifier],
                                         ontologyType: String): Try[String] = {
    val canonicalId = Identifiable.generate
    identifiersDao
      .saveIdentifier(
        Identifier(
          MiroID =
            findIdentifierWith(identifiers, IdentifierSchemes.miroImageNumber),
          CalmAltRefNo =
            findIdentifierWith(identifiers, IdentifierSchemes.calmAltRefNo),
          CanonicalID = canonicalId,
          ontologyType = ontologyType
        ))
      .map { _ =>
        canonicalId
      }
  }

  private def findIdentifierWith(identifiers: List[SourceIdentifier],
                                 identifierScheme: String): String = {
    identifiers
      .find(identifier => identifier.identifierScheme == identifierScheme)
      .fold[String](null)(identifier => identifier.value)
  }
}
