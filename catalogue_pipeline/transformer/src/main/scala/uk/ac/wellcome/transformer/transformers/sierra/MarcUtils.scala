package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData}

trait MarcUtils {
  def getMatchingSubfields(
    bibData: SierraBibData,
    marcTag: String,
    marcSubfieldTags: List[String]): List[MarcSubfield] = {
    bibData.varFields
      .filter { _.marcTag == Some(marcTag) }
      .map { _.subfields }
      .flatten
      .filter { subfield: MarcSubfield => marcSubfieldTags.contains(subfield.tag) }
  }

  def getMatchingSubfields(bibData: SierraBibData,
                           marcTag: String,
                           marcSubfieldTag: String): List[MarcSubfield] =
    getMatchingSubfields(
      bibData = bibData,
      marcTag = marcTag,
      marcSubfieldTags = List(marcSubfieldTag)
    )
}
