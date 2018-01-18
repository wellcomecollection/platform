package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData}

trait SierraDescription {

  def getSubfield(
    bibData: SierraBibData,
    marcTag: String,
    marcSubfieldTag: String
  ): Option[MarcSubfield] = {
    val matchingFields = bibData.varFields
      .filter {
        _.marcTag.contains(marcTag)
      }

    val matchingSubfields  =
      matchingFields.flatMap {
        _.subfields
      }.flatten

    matchingSubfields.find(_.tag == marcSubfieldTag)
  }

  def getDescription(bibData: SierraBibData): Option[String] = {
    val descriptionField = getSubfield(bibData, "520", "a")
    val summaryDescriptionField = getSubfield(bibData, "520", "b")

    (descriptionField, summaryDescriptionField) match {
      case (Some(a), Some(b)) => Some(s"${a.content} ${b.content}")
      case (Some(a), None) => Some(a.content)
      case (None, None) => None
    }

  }
}

