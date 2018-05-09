package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal.{AbstractConcept, Concept, Genre, MaybeDisplayable, Period, Place, Unidentifiable}
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData}

trait SierraGenres extends MarcUtils with SierraConcepts {

  // Populate wwork:genres
  //
  // Use MARC field "655".
  //
  // Within a MARC 655 tag, there's:
  //
  //    - a primary concept (subfield $a); and
  //    - subdivisions (subfields $v, $x, $y and $z)
  //
  // The primary concept can be identified, and the subdivisions serve
  // to add extra context.
  //
  // We construct the Genre as follows:
  //
  //    - label is the concatenation of $a, $v, $x, $y and $z in order,
  //      separated by a hyphen ' - '.
  //    - concepts is a List[Concept] populated in order of the subfields:
  //
  //        * $a => Concept
  //          Optionally with an identifier.  We look in subfield $0 for the
  //          identifier value, then second indicator for the authority.
  //
  //        * $v => Concept
  //        * $x => Concept
  //        * $y => Period
  //        * $z => Place
  //
  //      Note that only concepts from subfield $a are identified; everything
  //      else is unidentified.
  //
  def getGenres(
    bibData: SierraBibData): List[Genre[MaybeDisplayable[AbstractConcept]]] = {
    getGenresForMarcTag(bibData, "655")
  }

  private def getGenresForMarcTag(bibData: SierraBibData, marcTag: String) = {
    val subfieldsList =
      getMatchingSubfields(bibData, marcTag, List("a", "v", "x", "y", "z"))

    subfieldsList.map(subfields => {
      val (primarySubfields, subdivisionSubfields) = subfields.partition { _.tag == "a" }

      val label = getLabel(primarySubfields, subdivisionSubfields)
      val concepts: List[MaybeDisplayable[AbstractConcept]] = getPrimaryConcept(primarySubfields) ++ getSubdivisions(subdivisionSubfields)

      Genre[MaybeDisplayable[AbstractConcept]](
        label = label,
        concepts = concepts
      )
    })
  }

  // Extract the primary concept, which comes from subfield $a.  This is the
  // only concept which might be identified.
  private def getPrimaryConcept(primarySubfields: List[MarcSubfield]): List[MaybeDisplayable[Concept]] = {
    primarySubfields.map { subfield =>
      buildPrimaryConcept[Concept](subfield)
    }
  }

  // Extract the subdivisions, which come from everything except subfield $a.
  // These are never identified.  We preserve the order from MARC.
  private def getSubdivisions(subdivisionSubfields: List[MarcSubfield]): List[MaybeDisplayable[AbstractConcept]] = {
    subdivisionSubfields.map { subfield =>
      subfield.tag match {
        case "v" | "w" => Unidentifiable(Concept(label = subfield.content))
        case "y" => Unidentifiable(Period(label = subfield.content))
        case "z" => Unidentifiable(Place(label = subfield.content))
      }
    }
  }
}
