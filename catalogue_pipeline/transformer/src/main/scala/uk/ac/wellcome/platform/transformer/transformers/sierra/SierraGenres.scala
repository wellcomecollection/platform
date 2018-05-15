package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal.{
  AbstractConcept,
  Concept,
  Genre,
  MaybeDisplayable
}
import uk.ac.wellcome.platform.transformer.source.{
  MarcSubfield,
  SierraBibData,
  VarField
}

trait SierraGenres extends MarcUtils with SierraConcepts {

  private val marcTag = "655"

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
    getGenresForMarcTag(bibData, marcTag)
  }

  private def getGenresForMarcTag(bibData: SierraBibData, marcTag: String) = {
    val marcVarFields = getMatchingVarFields(bibData, marcTag = marcTag)

    marcVarFields.map { varField =>
      val subfields = varField.subfields.filter { subfield =>
        List("a", "v", "x", "y", "z").contains(subfield.tag)
      }
      val (primarySubfields, subdivisionSubfields) = subfields.partition {
        _.tag == "a"
      }

      val label = getLabel(primarySubfields, subdivisionSubfields)
      val concepts
        : List[MaybeDisplayable[AbstractConcept]] = getPrimaryConcept(
        primarySubfields,
        varField = varField) ++ getSubdivisions(subdivisionSubfields)

      Genre[MaybeDisplayable[AbstractConcept]](
        label = label,
        concepts = concepts
      )
    }
  }

  // Extract the primary concept, which comes from subfield $a.  This is the
  // only concept which might be identified.
  private def getPrimaryConcept(
    primarySubfields: List[MarcSubfield],
    varField: VarField): List[MaybeDisplayable[AbstractConcept]] = {
    primarySubfields.map { subfield =>
      identifyPrimaryConcept(
        concept = Concept(label = subfield.content),
        varField = varField
      )
    }
  }
}
