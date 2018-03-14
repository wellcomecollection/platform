package uk.ac.wellcome.platform.snapshot_convertor.models

case class CompletedConversionJob(conversionJob: ConversionJob, targetLocation: String)

object CompletedConversionJob {
  def apply(conversionJob: ConversionJob): CompletedConversionJob =
    CompletedConversionJob(conversionJob, "foo")
}
