package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.Agent

import uk.ac.wellcome.transformer.source.SierraBibData

trait SierraPublishers {

  // Populate wwork:publishers.
  //
  // We use MARC field "260", if populated, otherwise we use field "264".
  //
  // In either case:
  //  - "label" comes from subfield $b
  //  - "type" is "Organisation".
  //
  // Notes:
  //  - A bib may have 260, or 264, or neither -- but we would never expect
  //    it to have both.
  //  - A bib may have a single 260 field, or multiple 264 fields.
  //  - Subfield $b may occur more than once on a field.
  //
  // https://www.loc.gov/marc/bibliographic/bd260.html
  // https://www.loc.gov/marc/bibliographic/bd264.html
  //
  def getPublishers(bibData: SierraBibData): List[Agent] = {
    val matchingFields = bibData.varFields
      .filter { _.marcTag.contains("260") }

    val publisherFields = if (matchingFields.nonEmpty) {
      matchingFields
    } else {
      bibData.varFields
        .filter { _.marcTag.contains("264") }
    }

    val matchingSubfields = publisherFields.flatMap(_.subfields)

    matchingSubfields
      .filter { _.tag == "b" }
      .map { subfield =>
        Agent(
          label = subfield.content,
          ontologyType = "Organisation"
        )
      }
  }
}
