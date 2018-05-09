package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal.{AbstractConcept, Concept, MaybeDisplayable, Period, Place, Subject, Unidentifiable}
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData}

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
    val subfieldsList = getMatchingSubfields(
      bibData, marcTag, marcSubfieldTags = List("a", "v", "x", "y", "z"))

    subfieldsList.map(subfields => {
      val (primarySubfields, subdivisionSubfields) = subfields.partition { _.tag == "a" }

      val label = getLabel(primarySubfields, subdivisionSubfields)
      val concepts: List[MaybeDisplayable[AbstractConcept]] = getPrimaryConcept(marcTag, primarySubfields) ++ getSubdivisions(subdivisionSubfields)

      Subject[MaybeDisplayable[AbstractConcept]](
        label = label,
        concepts = concepts
      )
    })
  }

  // Extract the primary concept, which comes from subfield $a.  This is the
  // only concept which might be identified.
  private def getPrimaryConcept(marcTag: String, primarySubfields: List[MarcSubfield]): List[MaybeDisplayable[AbstractConcept]] = {
    primarySubfields.map { subfield =>
      marcTag match {
        case "650" => buildPrimaryConcept[Concept](subfield)
        case "648" => buildPrimaryConcept[Period](subfield)
        case "651" => buildPrimaryConcept[Place](subfield)
      }

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
