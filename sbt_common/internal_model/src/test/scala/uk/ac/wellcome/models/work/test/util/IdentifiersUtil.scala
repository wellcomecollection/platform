package uk.ac.wellcome.models.work.test.util

import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}

import scala.util.Random

trait IdentifiersUtil {
  def randomAlphanumeric(length: Int): String =
    (Random.alphanumeric take length mkString) toLowerCase

  def createCanonicalId: String = randomAlphanumeric(length = 10)

  def createSourceIdentifier: SourceIdentifier = createSourceIdentifierWith()

  def createSourceIdentifierWith(identifierType: String = "miro-image-number"): SourceIdentifier =
    SourceIdentifier(
      identifierType = IdentifierType(identifierType),
      value = randomAlphanumeric(length = 10),
      ontologyType = "Work"
    )
}
