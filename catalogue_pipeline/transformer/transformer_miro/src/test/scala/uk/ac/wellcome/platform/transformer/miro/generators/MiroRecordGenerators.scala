package uk.ac.wellcome.platform.transformer.miro.generators

import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

trait MiroRecordGenerators {
  def createMiroRecordWith(
    title: Option[String] = Some("Auto-created title in MiroRecordGenerators"),
    useRestrictions: Option[String] = Some("CC-BY"),
    innopacID: Option[String] = None,
    creditLine: Option[String] = None,
    sourceCode: Option[String] = None,
    imageNumber: String = "M0000001"
  ): MiroRecord =
    MiroRecord(
      title = title,
      copyrightCleared = Some("Y"),
      useRestrictions = useRestrictions,
      innopacID = innopacID,
      creditLine = creditLine,
      sourceCode = sourceCode,
      imageNumber = imageNumber
    )

  def createMiroRecord: MiroRecord =
    createMiroRecordWith()
}
