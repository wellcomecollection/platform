package uk.ac.wellcome.platform.archive.common.models.bagit

import java.time.LocalDate

case class BagInfo(
  externalIdentifier: ExternalIdentifier,
  sourceOrganisation: SourceOrganisation,
  payloadOxum: PayloadOxum,
  baggingDate: LocalDate,
  externalDescription: Option[ExternalDescription] = None,
  internalSenderIdentifier: Option[InternalSenderIdentifier] = None,
  internalSenderDescription: Option[InternalSenderDescription] = None)

case class InternalSenderIdentifier(underlying: String) extends AnyVal {
  override def toString: String = underlying
}
case class InternalSenderDescription(underlying: String) extends AnyVal {
  override def toString: String = underlying
}
case class ExternalDescription(underlying: String) extends AnyVal {
  override def toString: String = underlying
}
case class SourceOrganisation(underlying: String) extends AnyVal {
  override def toString: String = underlying
}

case class PayloadOxum(payloadBytes: Long, numberOfPayloadFiles: Int) {
  override def toString = s"$payloadBytes.$numberOfPayloadFiles"
}
