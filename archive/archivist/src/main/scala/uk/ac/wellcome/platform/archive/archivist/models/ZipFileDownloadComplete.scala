package uk.ac.wellcome.platform.archive.archivist.models

import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest

case class ZipFileDownloadComplete(zipFile: ZipFile,
                                   ingestBagRequest: IngestBagRequest)