package uk.ac.wellcome.platform.archive.archivist.models

import java.io.File

import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest

case class FileDownloadComplete(file: File, ingestBagRequest: IngestBagRequest)
