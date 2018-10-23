package uk.ac.wellcome.platform.archive.common.models

case class BagInfo(externalIdentifier: ExternalIdentifier, sourceOrganisation: SourceOrganisation, payloadOxum: PayloadOxum)

case class SourceOrganisation(underlying: String) extends AnyVal{
  override def toString: String = underlying
}

case class PayloadOxum(payloadBytes: Long, numberOfPayloadFiles: Int)