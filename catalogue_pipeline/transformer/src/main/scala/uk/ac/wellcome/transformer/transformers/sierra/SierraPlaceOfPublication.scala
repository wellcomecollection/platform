package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models._
import uk.ac.wellcome.transformer.source.{
  MarcSubfield,
  SierraBibData,
  VarField
}

trait SierraPlaceOfPublication {

  // Populate wwork:placeOfPublication.
  //
  // We use MARC field 260 subfield $a.
  //
  // Notes:
  //  - This field is repeatable, so we need to cope with multiple instances
  //    of this field.
  //
  def getPlacesOfPublication(bibData: SierraBibData): List[Place] = {
    bibData.varFields.collect {
      case VarField(_, None, Some("260"), _, _, subfields) =>
        subfields.collect {
          case MarcSubfield("a", content) =>
            Place(label = content)
        }
    }.flatten
  }
}
