package uk.ac.wellcome.platform.archive.common.generators

import java.time.LocalDate

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.bagit._

trait BagInfoGenerators extends RandomThings {

  def createBagInfoWith(
    externalIdentifier: ExternalIdentifier = randomExternalIdentifier,
    payloadOxum: PayloadOxum = randomPayloadOxum,
    baggingDate: LocalDate = randomLocalDate,
    sourceOrganisation: Option[SourceOrganisation] = Some(
      randomSourceOrganisation),
    externalDescription: Option[ExternalDescription] = Some(
      randomExternalDescription),
    internalSenderIdentifier: Option[InternalSenderIdentifier] = Some(
      randomInternalSenderIdentifier),
    internalSenderDescription: Option[InternalSenderDescription] = Some(
      randomInternalSenderDescription)
  ): BagInfo =
    BagInfo(
      externalIdentifier = externalIdentifier,
      payloadOxum = payloadOxum,
      baggingDate = baggingDate,
      sourceOrganisation = sourceOrganisation,
      externalDescription = externalDescription,
      internalSenderIdentifier = internalSenderIdentifier,
      internalSenderDescription = internalSenderDescription
    )

  def createBagInfo: BagInfo = createBagInfoWith()
}
