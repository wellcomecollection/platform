package uk.ac.wellcome.platform.archive.common.generators

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagInfo,
  ExternalDescription,
  ExternalIdentifier
}

trait BagInfoGenerators extends RandomThings {
  def createBagInfoWith(
    externalIdentifier: ExternalIdentifier = randomExternalIdentifier,
    externalDescription: Option[ExternalDescription] = Some(
      randomExternalDescription)
  ): BagInfo =
    BagInfo(
      externalIdentifier = randomExternalIdentifier,
      sourceOrganisation = randomSourceOrganisation,
      payloadOxum = randomPayloadOxum,
      baggingDate = randomLocalDate,
      externalDescription = externalDescription,
      internalSenderIdentifier = Some(randomInternalSenderIdentifier),
      internalSenderDescription = Some(randomInternalSenderDescription)
    )

  def createBagInfo: BagInfo = createBagInfoWith()
}
