package uk.ac.wellcome.platform.archive.archivist.models

case class ItemDigest(value: String,
                      algorithm: ItemDigest.DigestAlgorithm = ItemDigest.sha256)

case object ItemDigest {
  sealed trait DigestAlgorithm

  private val sha256String = "SHA-256"

  case object sha256 extends DigestAlgorithm {
    override def toString: String = sha256String
  }
}
