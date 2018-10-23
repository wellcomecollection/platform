package uk.ac.wellcome.platform.archive.common.models
import java.time.LocalDate

case class BagInfo(externalIdentifier: ExternalIdentifier,
                   sourceOrganisation: SourceOrganisation,
                   payloadOxum: PayloadOxum,
                   baggingDate: LocalDate)

case class SourceOrganisation(underlying: String) extends AnyVal {
  override def toString: String = underlying
}

case class PayloadOxum(payloadBytes: Long, numberOfPayloadFiles: Int)
