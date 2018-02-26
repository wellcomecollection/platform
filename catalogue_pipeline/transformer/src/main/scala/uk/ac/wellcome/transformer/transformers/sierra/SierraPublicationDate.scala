package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData}
import uk.ac.wellcome.models.Period

trait SierraPublicationDate {

  private def findSubfield(bibData: SierraBibData,
                           tag: String,
                           subfieldTag: String): Option[MarcSubfield] = {
    for {
      varField <- bibData.varFields if varField.marcTag.contains(tag)
      subfield <- varField.subfields if subfieldTag == subfield.tag
    } yield subfield
  }.headOption

  // Populate wwork:publicationDate.
  //
  // We use MARC field "260" and subfield "c"
  //
  // https://www.loc.gov/marc/bibliographic/bd260.html
  //
  def getPublicationDate(bibData: SierraBibData): Option[Period] =
    findSubfield(bibData, "260", "c").map { c =>
      Period(c.content)
    }

}
