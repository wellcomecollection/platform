package uk.ac.wellcome.platform.idminter.steps

import com.google.inject.Inject
import com.twitter.inject.{Logging, TwitterModuleFlags}
import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{SourceIdentifier, Work}
import uk.ac.wellcome.platform.idminter.database.{
  IdentifiersDao,
  UnableToMintIdentifierException
}
import uk.ac.wellcome.platform.idminter.model.Identifier
import uk.ac.wellcome.platform.idminter.utils.Identifiable

import scala.concurrent.Future
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.util.Try

class IdentifierGenerator @Inject()(identifiersDao: IdentifiersDao,
                                    metricsSender: MetricsSender,
                                    knownIdentifierSchemes: List[String])
    extends Logging
    with TwitterModuleFlags {

  def retrieveOrGenerateCanonicalId(identifiers: List[SourceIdentifier],
                                    ontologyType: String): Try[String] = {
    Try {
      val idsWithKnownSchemes = identifiers.filter(identifier =>
        knownIdentifierSchemes.contains(identifier.identifierScheme))
      if (idsWithKnownSchemes.isEmpty) {
        throw UnableToMintIdentifierException(
          "identifiers list did not contain a known identifierScheme")
      } else {
        identifiersDao.lookupID(idsWithKnownSchemes, ontologyType).flatMap {
          case Some(id) =>
            metricsSender.incrementCount("found-old-id")
            Try(id.CanonicalID)
          case None =>
            val result = generateAndSaveCanonicalId(idsWithKnownSchemes)
            if (result.isSuccess)
              metricsSender.incrementCount("generated-new-id")

            result
        }
      }
    }.flatten
  }

  private def generateAndSaveCanonicalId(
    identifiers: List[SourceIdentifier]): Try[String] = {
    val canonicalId = Identifiable.generate
    identifiersDao
      .saveIdentifier(
        Identifier(
          MiroID =
            findIdentifierWith(identifiers, IdentifierSchemes.miroImageNumber),
          CalmAltRefNo =
            findIdentifierWith(identifiers, IdentifierSchemes.calmAltRefNo),
          CanonicalID = canonicalId
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

class SomethingSomethin @Inject()(metricsSender: MetricsSender,
                                  identifierGenerator: IdentifierGenerator)
    extends Logging {
  def generateId(work: Work): Future[String] = {
    metricsSender.timeAndCount(
      "generate-id",
      () =>
        findMiroID(work) match {
          case Some(identifier) =>
            Future {
              identifierGenerator
                .retrieveOrGenerateCanonicalId(
                  List(identifier),
                  ontologyType = work.ontologyType
                )
                .get
            }
          case None =>
            error(s"Item $work did not contain a MiroID")
            Future.failed(new Exception(s"Item $work did not contain a MiroID"))
      }
    )
  }

  private def findMiroID(work: Work): Option[SourceIdentifier] = {
    val maybeSourceIdentifier =
      work.identifiers.find(identifier =>
        identifier.identifierScheme == IdentifierSchemes.miroImageNumber)
    info(s"SourceIdentifier: $maybeSourceIdentifier")
    maybeSourceIdentifier
  }

}
