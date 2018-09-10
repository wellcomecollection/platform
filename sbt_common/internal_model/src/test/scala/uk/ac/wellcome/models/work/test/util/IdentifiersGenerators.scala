package uk.ac.wellcome.models.work.test.util

import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}

import scala.util.Random

trait IdentifiersGenerators {
  def randomAlphanumeric(length: Int): String =
    (Random.alphanumeric take length mkString) toLowerCase

  def createCanonicalId: String = randomAlphanumeric(length = 10)

  def createSourceIdentifier: SourceIdentifier = createSourceIdentifierWith()

  def createSourceIdentifierWith(
    identifierType: String = "miro-image-number",
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"): SourceIdentifier =
    SourceIdentifier(
      identifierType = IdentifierType(identifierType),
      value = value,
      ontologyType = ontologyType
    )

  def createSierraSourceIdentifier = createSierraSourceIdentifierWith()

  def createSierraSourceIdentifierWith(
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"): SourceIdentifier =
    SourceIdentifier(
      identifierType = IdentifierType("sierra-system-number"),
      value = value,
      ontologyType = ontologyType
    )

  def createMiroSourceIdentifier = createMiroSourceIdentifierWith()

  def createMiroSourceIdentifierWith(
                                        value: String = randomAlphanumeric(length = 10),
                                        ontologyType: String = "Work"): SourceIdentifier =
    SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      value = value,
      ontologyType = ontologyType
    )
}
