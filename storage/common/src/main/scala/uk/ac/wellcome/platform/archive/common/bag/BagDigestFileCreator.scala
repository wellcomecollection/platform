package uk.ac.wellcome.platform.archive.common.bag

import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagDigestFile,
  BagItemPath
}
import uk.ac.wellcome.platform.archive.common.models.error.{
  ArchiveError,
  InvalidBagManifestError
}

object BagDigestFileCreator {

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
  def create[T]( line: String,
                 job: T,
                 bagRootPathInZip: Option[String],
                 manifestName: String
  ): Either[ArchiveError[T], BagDigestFile] = {
    val checksumLineRegex = """(.+?)\s+(.+)""".r

    line match {
      case checksumLineRegex(checksum, itemPath) =>
        Right(
          BagDigestFile(
            checksum = checksum.trim,
            path = BagItemPath(bagRootPathInZip, itemPath.trim)
          )
        )
      case _ => Left(InvalidBagManifestError(job, manifestName, line))
    }
  }
}
