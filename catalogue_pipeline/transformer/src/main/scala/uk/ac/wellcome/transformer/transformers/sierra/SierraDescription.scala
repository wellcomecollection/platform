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
  // The value comes from comes subfield $a concatenated with subfield $b.
  //
  // Notes:
  //  - A bib may have multiple 520 records, in which case we join with spaces
  //  - If $b is empty, we just use $a
  //  - We never expect to see a record with $b but not $a
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

          // We never expect to see this in practice.  If we do, we should
          // refuse to process it, and if/when we see it we can decide how
          // it should be handled.  For now, just throw an exception.
          case (None, Some(b)) =>
            throw new RuntimeException(
              s"Saw a MARC field 520 with $$b but no $$a? $bibData"
            )
        }
      }) match {
      case Nil => None
      case list => Some(list.mkString(" "))
    }
  }
}
