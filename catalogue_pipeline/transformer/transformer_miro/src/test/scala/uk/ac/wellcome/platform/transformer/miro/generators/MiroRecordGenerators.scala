package uk.ac.wellcome.platform.transformer.miro.generators

import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

trait MiroRecordGenerators {
  def createMiroRecordWith(
    title: Option[String] = Some("Auto-created title in MiroRecordGenerators"),
    creator: Option[List[Option[String]]] = None,
    description: Option[String] = None,
    academicDescription: Option[String] = None,
    secondaryCreator: Option[List[String]] = None,
    artworkDate: Option[String] = None,
    copyrightCleared: Option[String] = Some("Y"),
    keywords: Option[List[String]] = None,
    keywordsUnauth: Option[List[Option[String]]] = None,
    physFormat: Option[String] = None,
    lcGenre: Option[String] = None,
    useRestrictions: Option[String] = Some("CC-BY"),
    suppLettering: Option[String] = None,
    innopacID: Option[String] = None,
    creditLine: Option[String] = None,
    sourceCode: Option[String] = None,
    libraryRefDepartment: List[Option[String]] = Nil,
    libraryRefId: List[Option[String]] = Nil,
    award: List[Option[String]] = Nil,
    awardDate: List[Option[String]] = Nil,
    imageNumber: String = "M0000001"
  ): MiroRecord =
    MiroRecord(
      title = title,
      creator = creator,
      description = description,
      academicDescription = academicDescription,
      secondaryCreator = secondaryCreator,
      artworkDate = artworkDate,
      copyrightCleared = copyrightCleared,
      keywords = keywords,
      keywordsUnauth = keywordsUnauth,
      physFormat = physFormat,
      lcGenre = lcGenre,
      useRestrictions = useRestrictions,
      suppLettering = suppLettering,
      innopacID = innopacID,
      creditLine = creditLine,
      sourceCode = sourceCode,
      libraryRefDepartment = libraryRefDepartment,
      libraryRefId = libraryRefId,
      award = award,
      awardDate = awardDate,
      imageNumber = imageNumber
    )

  def createMiroRecord: MiroRecord =
    createMiroRecordWith()
}
