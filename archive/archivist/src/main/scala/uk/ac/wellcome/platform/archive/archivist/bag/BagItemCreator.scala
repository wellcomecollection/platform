package uk.ac.wellcome.platform.archive.archivist.bag
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveJob
import uk.ac.wellcome.platform.archive.common.models.error.{
  ArchiveError,
  InvalidBagManifestError
}
import uk.ac.wellcome.platform.archive.common.models.{BagItem, EntryPath}

object BagItemCreator {

  /** Within a manifest, each entry in the bag is a single line, containing
    * a checksum and the location.  For example:
    *
    *     676...8c32  data/b12345678.xml
    *     593...5c5  data/alto/b12345678_0001.xml
    *     26a...78c  data/alto/b12345678_0002.xml
    *
    * Given a single line, this method extracts the checksum and the
    * location, or returns an error if the line is incorrectly formatted.
    *
    */
  def create(
    line: String,
    job: ArchiveJob,
    manifestName: String
  ): Either[ArchiveError[ArchiveJob], BagItem] = {
    val checksumLineRegex = """(.+?)\s+(.+)""".r

    line match {
      case checksumLineRegex(checksum, key) => Right(
        BagItem(
          checksum = checksum.trim,
          location = EntryPath(key.trim)
        )
      )
      case _ => Left(InvalidBagManifestError(job, manifestName))
    }
  }
}
