package uk.ac.wellcome.platform.transformer.sierra.transformers.subjects

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.source.{
  MarcSubfield,
  SierraBibData,
  VarField
}
import uk.ac.wellcome.platform.transformer.sierra.transformers.{
  MarcUtils,
  SierraConcepts
}

trait SierraConceptSubjects extends MarcUtils with SierraConcepts {

  // Populate wwork:subject
  //
  // Use MARC field "650", "648" and "651" where the second indicator is not 7.
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
  def getSubjectswithAbstractConcepts(
    bibData: SierraBibData): List[Subject[MaybeDisplayable[AbstractConcept]]] =
    getSubjectsForMarcTag(bibData, "650") ++
      getSubjectsForMarcTag(bibData, "648") ++
      getSubjectsForMarcTag(bibData, "651")

  private def getSubjectsForMarcTag(
    bibData: SierraBibData,
    marcTag: String): List[Subject[MaybeDisplayable[AbstractConcept]]] = {
    val marcVarFields = getMatchingVarFields(bibData, marcTag = marcTag)

    // Second indicator 7 means that the subject authority is something other
    // than library of congress or mesh. Some MARC records have duplicated subjects
    // when the same subject has more than one authority (for example mesh and FAST),
    // which causes duplicated subjects to appear in the API.
    // So let's filter anything that is from another authority for now.
    marcVarFields.filterNot(_.indicator2.contains("7")).map { varField =>
      val subfields = filterSubfields(varField, List("a", "v", "x", "y", "z"))
      val (primarySubfields, subdivisionSubfields) = subfields.partition {
        _.tag == "a"
      }

      val label = getLabel(primarySubfields, subdivisionSubfields)
      val concepts: List[MaybeDisplayable[AbstractConcept]] = getPrimaryConcept(
        primarySubfields,
        varField = varField) ++ getSubdivisions(subdivisionSubfields)

      Subject(
        label = label,
        concepts = concepts
      )
    }
  }

  private def filterSubfields(varField: VarField, subfields: List[String]) = {
    varField.subfields.filter { subfield =>
      subfields.contains(subfield.tag)
    }
  }

  private def getPrimaryConcept(
    primarySubfields: List[MarcSubfield],
    varField: VarField): List[MaybeDisplayable[AbstractConcept]] = {
    primarySubfields.map { subfield =>
      varField.marcTag.get match {
        case "650" =>
          identifyPrimaryConcept(
            concept = Concept(label = subfield.content),
            varField = varField
          )
        case "648" =>
          identifyPrimaryConcept(
            concept = Period(label = subfield.content),
            varField = varField
          )
        case "651" =>
          identifyPrimaryConcept(
            concept = Place(label = subfield.content),
            varField = varField
          )
      }

    }
  }
}
