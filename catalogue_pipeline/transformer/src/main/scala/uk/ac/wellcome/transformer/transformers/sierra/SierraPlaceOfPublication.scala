package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models._
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData, VarField}

trait SierraPlaceOfPublication {
  def getPlacesOfPublication(
    bibData: SierraBibData): List[PlaceOfPublication] = {
    bibData.varFields.collect {
      case VarField(_, None, Some("260"), _, _, subfields) =>
        subfields.collect {
          case MarcSubfield("a", content) =>
            PlaceOfPublication(label = content)
        }
    }.flatten
  }
}
