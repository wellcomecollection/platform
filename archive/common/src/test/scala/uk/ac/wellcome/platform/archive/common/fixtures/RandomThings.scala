package uk.ac.wellcome.platform.archive.common.fixtures

import java.util.UUID

import uk.ac.wellcome.platform.archive.common.models.{
  BagId,
  ExternalIdentifier,
  StorageSpace
}

import scala.util.Random

trait RandomThings {
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

  def randomStorageSpace = StorageSpace(
    randomAlphanumeric()
  )

  def randomBagId = BagId(
    randomStorageSpace,
    randomExternalIdentifier
  )
}
