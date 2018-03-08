package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models._
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData, VarField}

trait SierraPlaceOfPublication {
  def getPlaceOfPublications(
    bibData: SierraBibData): List[PlaceOfPublication] = {
    val unidentifiablePlaceOfPublications = bibData.varFields.collect {
      case VarField(_, None, Some("260"), _, _, subfields) =>
        subfields.collect {
          case MarcSubfield("a", content) =>
            UnidentifiablePlaceOfPublication(label = content)
        }
    }.flatten

    val maybeIdentifiablePlaceOfPublication = bibData.country.map(country => {
      val sourceIdentifier =
        SourceIdentifier(IdentifierSchemes.marcCountries, country.code)
      IdentifiablePlaceOfPublication(
        country.name,
        sourceIdentifier = sourceIdentifier,
        identifiers = List(sourceIdentifier))
    })

    unidentifiablePlaceOfPublications ++ maybeIdentifiablePlaceOfPublication.toList
  }
}
