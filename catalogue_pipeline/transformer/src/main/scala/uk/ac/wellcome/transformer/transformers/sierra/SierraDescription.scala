package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData}

trait SierraDescription {

  def getSubfields(
    bibData: SierraBibData,
    marcTag: String,
    marcSubfieldTags: List[String]
  ): List[Map[String, MarcSubfield]] = {
    val matchingFields = bibData.varFields
      .filter {
        _.marcTag.contains(marcTag)
      }

    matchingFields.map(varField => {
      varField.subfields
        .filter(subfield => marcSubfieldTags.contains(subfield.tag))
        .map(subfield => subfield.tag -> subfield)
        .toMap
    })
  }

  // Populate wwork:description.
  //
  // We use MARC field "520"
  //
  // The value comes from comes subfield $a concatenated with subfield $b
  //
  // Notes:
  //  - A bib may have multiple 520 records
  //  - If subfield $b is empty,
  //
  // https://www.loc.gov/marc/bibliographic/bd520.html
  //
  def getDescription(bibData: SierraBibData): Option[String] = {
    getSubfields(bibData, "520", List("a", "b"))
      .foldLeft[List[String]](Nil)((acc, subfields) => {

        (subfields.get("a"), subfields.get("b")) match {
          case (Some(a), Some(b)) => acc :+ s"${a.content} ${b.content}"
          case (Some(a), None) => acc :+ a.content
          case (None, None) => acc
        }
      }) match {
      case Nil => None
      case list => Some(list.mkString(" "))
    }
  }
}
