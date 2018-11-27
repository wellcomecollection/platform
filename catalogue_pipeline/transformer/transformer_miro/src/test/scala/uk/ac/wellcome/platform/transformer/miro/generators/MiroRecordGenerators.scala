package uk.ac.wellcome.platform.transformer.miro.generators

import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

trait MiroRecordGenerators {
  def createMiroRecordWith(
    useRestrictions: Option[String] = None,
    innopacID: Option[String] = None,
    sourceCode: Option[String] = None,
    imageNumber: String = "M0000001"
  ): MiroRecord =
    MiroRecord(
      useRestrictions = useRestrictions,
      innopacID = innopacID,
      sourceCode = sourceCode,
      imageNumber = imageNumber
    )
}
