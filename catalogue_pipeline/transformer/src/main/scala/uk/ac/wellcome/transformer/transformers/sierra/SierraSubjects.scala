package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData, VarField}

trait SierraSubjects extends MarcUtils with SierraConcepts {

  def getSubjects(bibData: SierraBibData)
    : List[Subject[MaybeDisplayable[AbstractConcept]]] = {
    getSubjectsForMarcTag(bibData, "650") ++
      getSubjectsForMarcTag(bibData, "648") ++
      getSubjectsForMarcTag(bibData, "651")
  }

  // Populate wwork:subject
  //
  // Use MARC field "650", "648" and "651".
  //
  // Within these MARC tags, we have:
  //
  //    - a primary concept (subfield $a); and
  //    - subdivisions (subfields $v, $x, $y and $z)
  //
  // The primary concept can be identified, and the subdivisions serve
  // to add extra context.
  //
  // We construct the Subject as follows:
  //
  //    - label is the concatenation of $a, $v, $x, $y and $z in order,
  //      separated by a hyphen ' - '.
  //    - concepts is a List[Concept] populated in order of the subfields:
  //
  //        * $a => {Concept, Period, Place}
  //          Optionally with an identifier.  We look in subfield $0 for the
  //          identifier value, then second indicator for the authority.
  //          These are decided as follows:
  //
  //            - 650 => Concept
  //            - 648 => Period
  //            - 651 => Place
  //
  //        * $v => Concept
  //        * $x => Concept
  //        * $y => Period
  //        * $z => Place
  //
  //      Note that only concepts from subfield $a are identified; everything
  //      else is unidentified.
  //
  private def getSubjectsForMarcTag(bibData: SierraBibData, marcTag: String) = {
    val marcVarFields = getMatchingVarFields(bibData, marcTag = marcTag)

    marcVarFields.map { varField =>
      val subfields = varField.subfields.filter { subfield =>
        List("a", "v", "x", "y", "z").contains( subfield.tag )
      }
      val (primarySubfields, subdivisionSubfields) = subfields.partition { _.tag == "a" }

      val label = getLabel(primarySubfields, subdivisionSubfields)
      val concepts: List[MaybeDisplayable[AbstractConcept]] = getPrimaryConcept(primarySubfields, varField = varField) ++ getSubdivisions(subdivisionSubfields)

      Subject[MaybeDisplayable[AbstractConcept]](
        label = label,
        concepts = concepts
      )
    }
  }

  // Extract the primary concept, which comes from subfield $a.  This is the
  // only concept which might be identified.
  private def getPrimaryConcept(primarySubfields: List[MarcSubfield], varField: VarField): List[MaybeDisplayable[AbstractConcept]] = {
    primarySubfields.map { subfield =>
      varField.marcTag.get match {
        case "650" => identifyPrimaryConcept(
          concept = Concept(label = subfield.content),
          varField = varField
        )
        case "648" => identifyPrimaryConcept(
          concept = Period(label = subfield.content),
          varField = varField
        )
        case "651" => identifyPrimaryConcept(
          concept = Place(label = subfield.content),
          varField = varField
        )
      }

    }
  }
}
