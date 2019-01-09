package uk.ac.wellcome.platform.archive.common.models

import java.io.File

case class FileDownloadComplete(file: File, ingestBagRequest: IngestBagRequest)
