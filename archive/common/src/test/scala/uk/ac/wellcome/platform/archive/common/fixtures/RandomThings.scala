package uk.ac.wellcome.platform.archive.common.fixtures

import java.time.LocalDate
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.models._

import scala.util.Random

trait RandomThings {
  def randomBagInfo =
    BagInfo(
      randomExternalIdentifier,
      randomSourceOrganisation,
      randomPayloadOxum,
      randomLocalDate)

  def randomAlphanumeric(length: Int = 8) = {
    Random.alphanumeric take length mkString
  }

  def randomPort = {
    val startPort = 10000
    val portRange = 10000

    startPort + Random.nextInt(portRange)
  }

  def randomUUID = UUID.randomUUID()

  def randomExternalIdentifier =
    ExternalIdentifier(randomAlphanumeric())

  def randomSourceOrganisation =
    SourceOrganisation(randomAlphanumeric())

  def randomPayloadOxum =
    PayloadOxum(Random.nextLong().abs, Random.nextInt().abs)

  def randomLocalDate = {
    val startRange = -999999999
    val maxValue = 1999999998
    LocalDate.ofEpochDay(startRange + Random.nextInt(maxValue))
  }

  def randomStorageSpace = StorageSpace(
    randomAlphanumeric()
  )

  def randomBagId = BagId(
    randomStorageSpace,
    randomExternalIdentifier
  )
}
