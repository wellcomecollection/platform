package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.Agent

import uk.ac.wellcome.transformer.source.SierraBibData

trait SierraPublishers {

  // Populate wwork:publishers.
  //
  //    For bibliographic records where "260" is populated:
  //    - "label" comes from MARC field 260 subfield $b.
  //    - "type" is "Organisation"
  //
  // Note that subfield $b can occur more than once on a record.
  //
  // http://www.loc.gov/marc/bibliographic/bd260.html
  def getPublishers(bibData: SierraBibData): List[Agent] = {
    val matchingSubfields = bibData.varFields
      .filter(_.marcTag.contains("260"))
      .flatMap {
        _.subfields
      }
      .flatten

    matchingSubfields
      .filter {
        _.tag == "b"
      }
      .map { subfield =>
        Agent(
          label = subfield.content,
          ontologyType = "Organisation"
        )
      }
  }
}
