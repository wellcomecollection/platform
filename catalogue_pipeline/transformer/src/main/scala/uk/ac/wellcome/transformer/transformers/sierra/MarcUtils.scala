package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.transformer.source.{
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

  def getMatchingSubfields(bibData: SierraBibData,
                           marcTag: String,
                           marcSubfieldTag: String): List[List[MarcSubfield]] =
    getMatchingSubfields(
      bibData = bibData,
      marcTag = marcTag,
      marcSubfieldTags = List(marcSubfieldTag)
    )
}
