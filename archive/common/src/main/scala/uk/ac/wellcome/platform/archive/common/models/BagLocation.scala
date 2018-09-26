package uk.ac.wellcome.platform.archive.common.models

case class BagLocation(
  storageNamespace: String,
  storagePath: String,
  bagPath: BagPath
)
