package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.transformer.source.MarcSubfield

trait SierraConcepts {

  // Get the label.  This is populated by the label of subfield $a, followed
  // by other subfields, in the order they come from MARC.  The labels are
  // joined by " - ".
  protected def getLabel(primarySubfields: List[MarcSubfield], subdivisionSubfields: List[MarcSubfield]): String = {
    val orderedSubfields = primarySubfields ++ subdivisionSubfields
    orderedSubfields.map { _.content }.mkString(" - ")
  }

  protected def buildPrimaryConcept[T <: AbstractConcept](subfield: MarcSubfield): MaybeDisplayable[AbstractConcept] =
    Unidentifiable(T(label = subfield.content))

  // Extract the subdivisions, which come from everything except subfield $a.
  // These are never identified.  We preserve the order from MARC.
  protected def getSubdivisions(subdivisionSubfields: List[MarcSubfield]): List[MaybeDisplayable[AbstractConcept]] = {
    subdivisionSubfields.map { subfield =>
      subfield.tag match {
        case "v" | "x" => Unidentifiable(Concept(label = subfield.content))
        case "y" => Unidentifiable(Period(label = subfield.content))
        case "z" => Unidentifiable(Place(label = subfield.content))
      }
    }
  }
}
