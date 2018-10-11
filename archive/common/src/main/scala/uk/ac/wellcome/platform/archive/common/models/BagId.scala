package uk.ac.wellcome.platform.archive.common.models

case class BagId(
                  space: StorageSpace,
                  externalIdentifier: ExternalIdentifier
                )

case class ExternalIdentifier(underlying: String) extends AnyVal {
  override def toString: String = underlying
}


