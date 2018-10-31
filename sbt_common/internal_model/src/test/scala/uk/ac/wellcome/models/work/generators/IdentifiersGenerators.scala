package uk.ac.wellcome.models.work.generators

import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}

import scala.util.Random

trait IdentifiersGenerators {
  def randomAlphanumeric(length: Int): String =
    (Random.alphanumeric take length mkString) toLowerCase

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

  def createSierraSystemSourceIdentifier: SourceIdentifier =
    createSierraSystemSourceIdentifierWith()

  def createSierraSystemSourceIdentifierWith(
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"): SourceIdentifier =
    SourceIdentifier(
      identifierType = createSierraSystemSourceIdentifierType,
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

  def createMiroLibraryReferenceSourceIdentifier: SourceIdentifier =
    createSourceIdentifierWith(
      identifierType = createMiroLibrarySourceIdentifierType)

  def createSierraIdentifierSourceIdentifier: SourceIdentifier =
    createSourceIdentifierWith(
      identifierType = createSierraIdentifierSourceIdentifierType)

  def createSierraSystemSourceIdentifierType: IdentifierType = {
    IdentifierType("sierra-system-number")
  }

  def createSierraIdentifierSourceIdentifierType: IdentifierType = {
    IdentifierType("sierra-identifier")
  }

  def createIsbnSourceIdentifierType: IdentifierType = {
    IdentifierType("isbn")
  }

  def createMiroLibrarySourceIdentifierType: IdentifierType = {
    IdentifierType("miro-library-reference")
  }

  def createCalmSourceIdentifierType: IdentifierType = {
    IdentifierType("calm-altref-no")
  }
}
