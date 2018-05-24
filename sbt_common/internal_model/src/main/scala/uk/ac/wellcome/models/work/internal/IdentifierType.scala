package uk.ac.wellcome.models.work.internal

import uk.ac.wellcome.models.utils.CSVReferenceTypeManager

case class IdentifierType(
  id: String,
  label: String,
  ontologyType: String = "IdentifierType"
)

case object IdentifierType {
  private val manager = new CSVReferenceTypeManager("/identifier-schemes.csv")

  def apply(platformId: String): IdentifierType = {
    val referenceType = manager.lookupId(platformId)
    IdentifierType(
      id = referenceType.id,
      label = referenceType.label
    )
  }
}
