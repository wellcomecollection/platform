package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.platform.transformer.source.{
  MarcSubfield,
  SierraBibData,
  VarField
}

trait MarcUtils {
  def getMatchingSubfields(
    bibData: SierraBibData,
    marcTag: String,
    marcSubfieldTags: List[String]): List[List[MarcSubfield]] =
    bibData.varFields
      .collect {
        case VarField(_, _, Some(m), _, _, subfields) if m == marcTag =>
          subfields.filter { subfield =>
            marcSubfieldTags.contains(subfield.tag)
          }
      }

  def getMatchingVarFields(bibData: SierraBibData,
                           marcTag: String): List[VarField] =
    bibData.varFields
      .filter { _.marcTag == Some(marcTag) }

  def getMatchingSubfields(bibData: SierraBibData,
                           marcTag: String,
                           marcSubfieldTag: String): List[List[MarcSubfield]] =
    getMatchingSubfields(
      bibData = bibData,
      marcTag = marcTag,
      marcSubfieldTags = List(marcSubfieldTag)
    )
}
