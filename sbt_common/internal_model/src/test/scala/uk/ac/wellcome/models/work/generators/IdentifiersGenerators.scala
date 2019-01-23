package uk.ac.wellcome.models.work.generators

import uk.ac.wellcome.models.generators.RandomStrings
import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}

trait IdentifiersGenerators extends RandomStrings {
  def createCanonicalId: String = randomAlphanumeric(length = 10)

  def createSourceIdentifier: SourceIdentifier = createSourceIdentifierWith()

  def createSourceIdentifierWith(
    identifierType: IdentifierType = IdentifierType("miro-image-number"),
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"): SourceIdentifier =
    SourceIdentifier(
      identifierType = identifierType,
      value = value,
      ontologyType = ontologyType
    )

  def createSierraSystemSourceIdentifierWith(
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"
  ): SourceIdentifier =
    SourceIdentifier(
      identifierType = IdentifierType("sierra-system-number"),
      value = value,
      ontologyType = ontologyType
    )

  def createSierraSystemSourceIdentifier: SourceIdentifier =
    createSierraSystemSourceIdentifierWith()

  def createSierraIdentifierSourceIdentifierWith(
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"
  ): SourceIdentifier =
    SourceIdentifier(
      identifierType = IdentifierType("sierra-identifier"),
      value = value,
      ontologyType = ontologyType
    )

  def createSierraIdentifierSourceIdentifier: SourceIdentifier =
    createSierraIdentifierSourceIdentifierWith()

  def createIsbnSourceIdentifier: SourceIdentifier =
    createSourceIdentifierWith(
      identifierType = IdentifierType("isbn")
    )

  def createMiroSourceIdentifierWith(
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"
  ): SourceIdentifier =
    SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      ontologyType = ontologyType,
      value = value
    )

  def createMiroSourceIdentifier: SourceIdentifier =
    createMiroSourceIdentifierWith()
}
