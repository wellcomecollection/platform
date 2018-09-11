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

  def createSierraSourceIdentifier: SourceIdentifier =
    createSierraSourceIdentifierWith()

  def createSierraSourceIdentifierWith(
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"): SourceIdentifier =
    SourceIdentifier(
      identifierType = createSierraSourceIdentifierType,
      value = value,
      ontologyType = ontologyType
    )

  def createIsbnSourceIdentifier: SourceIdentifier =
    createIsbnSourceIdentifierWith()

  def createIsbnSourceIdentifierWith(
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"): SourceIdentifier =
    SourceIdentifier(
      identifierType = createIsbnSourceIdentifierType,
      value = value,
      ontologyType = ontologyType
    )

  def createMiroSourceIdentifier: SourceIdentifier =
    createMiroSourceIdentifierWith()

  def createMiroSourceIdentifierWith(
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"): SourceIdentifier =
    SourceIdentifier(
      identifierType = createMiroSourceIdentifierType,
      value = value,
      ontologyType = ontologyType
    )

  def createMiroLibraryReferenceIdentifier: SourceIdentifier =
    createMiroLibraryReferenceIdentifierWith()

  def createMiroLibraryReferenceIdentifierWith(
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"): SourceIdentifier =
    SourceIdentifier(
      identifierType = IdentifierType("miro-library-reference"),
      value = value,
      ontologyType = ontologyType
    )

  def createIsbnSourceIdentifierType: IdentifierType = {
    IdentifierType("isbn")
  }

  def createSierraSourceIdentifierType: IdentifierType = {
    IdentifierType("sierra-system-number")
  }

  def createMiroSourceIdentifierType: IdentifierType = {
    IdentifierType("miro-image-number")
  }
}
