package uk.ac.wellcome.platform.snapshot_convertor.models

case class CompletedConversionJob()

object CompletedConversionJob {
  def apply(conversionJob: ConversionJob): CompletedConversionJob =
    CompletedConversionJob()
}
