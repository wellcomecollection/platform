package uk.ac.wellcome.platform.archive.archivist.bag
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveJob
import uk.ac.wellcome.platform.archive.archivist.models.errors.{
  ArchiveError,
  InvalidBagManifestError
}
import uk.ac.wellcome.platform.archive.common.models.{BagItem, EntryPath}

object BagItemCreator {
  def create(
    fileChunk: String,
    job: ArchiveJob,
    manifestName: String,
    delimiter: String
  ): Either[ArchiveError[ArchiveJob], BagItem] = {
    val splitChunk = fileChunk.split(delimiter).map(_.trim)

    splitChunk match {
      case Array(checksum: String, key: String) =>
        Right(
          BagItem(
            checksum,
            EntryPath(key)
          ))
      case _ =>
        Left(InvalidBagManifestError(job, manifestName))
    }
  }
}
