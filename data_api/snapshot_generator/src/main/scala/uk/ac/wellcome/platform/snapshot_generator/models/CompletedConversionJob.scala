package uk.ac.wellcome.platform.snapshot_generator.models

import akka.http.scaladsl.model.Uri

case class CompletedConversionJob(
  conversionJob: ConversionJob,
  targetLocation: Uri
)
