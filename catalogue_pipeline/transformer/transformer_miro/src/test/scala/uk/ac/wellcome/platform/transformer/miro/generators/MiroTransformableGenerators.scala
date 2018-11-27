package uk.ac.wellcome.platform.transformer.miro.generators

import uk.ac.wellcome.platform.transformer.miro.models.MiroTransformable
import uk.ac.wellcome.platform.transformer.miro.source.MiroTransformableData

trait MiroTransformableGenerators {
  def createMiroTransformableWith(
    miroId: String = "M0000001",
    data: String
  ): MiroTransformable =
    MiroTransformable(
      sourceId = miroId,
      data = data
    )

  def createMiroTransformableDataWith(
    innopacID: Option[String] = None,
    useRestrictions: Option[String] = None,
    sourceCode: Option[String] = None,
    libraryRefDepartment: List[Option[String]] = Nil,
    libraryRefId: List[Option[String]] = Nil): MiroTransformableData =
    MiroTransformableData(
      copyrightCleared = Some("Y"),
      miroId = "M0000001",
      innopacID = innopacID,
      useRestrictions = useRestrictions,
      sourceCode = sourceCode,
      libraryRefDepartment = libraryRefDepartment,
      libraryRefId = libraryRefId
    )

  def createMiroTransformableData: MiroTransformableData =
    createMiroTransformableDataWith()

  def buildJSONForWork(miroId: String = "M0000001", extraData: String): String = {
    val baseData =
      s"""
         |  "image_no_calc": "$miroId",
         |  "image_cleared": "Y",
         |  "image_copyright_cleared": "Y",
         |  "image_tech_file_size": ["1000000"],
         |  "image_use_restrictions": "CC-BY"
      """.stripMargin

    if (extraData.isEmpty) s"""{$baseData}"""
    else s"""{
        $baseData,
        $extraData
      }"""
  }
}
