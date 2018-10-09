package uk.ac.wellcome.platform.archive.common.models

case class BagItem(checksum: String, location: EntryPath)

case class EntryPath(path: String)
