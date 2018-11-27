package uk.ac.wellcome.platform.transformer.miro.generators

import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

trait MiroRecordGenerators {
  def createMiroRecordWith(
    title: Option[String] = Some("Auto-created title in MiroRecordGenerators"),
    creator: Option[List[Option[String]]] = None,
    secondaryCreator: Option[List[String]] = None,
    keywords: Option[List[String]] = None,
    keywordsUnauth: Option[List[Option[String]]] = None,
    physFormat: Option[String] = None,
    lcGenre: Option[String] = None,
    useRestrictions: Option[String] = Some("CC-BY"),
    innopacID: Option[String] = None,
    creditLine: Option[String] = None,
    sourceCode: Option[String] = None,
    imageNumber: String = "M0000001"
  ): MiroRecord =
    MiroRecord(
      title = title,
      creator = creator,
      secondaryCreator = secondaryCreator,
      copyrightCleared = Some("Y"),
      keywords = keywords,
      keywordsUnauth = keywordsUnauth,
      physFormat = physFormat,
      lcGenre = lcGenre,
      useRestrictions = useRestrictions,
      innopacID = innopacID,
      creditLine = creditLine,
      sourceCode = sourceCode,
      imageNumber = imageNumber
    )

  def createMiroRecord: MiroRecord =
    createMiroRecordWith()
}
