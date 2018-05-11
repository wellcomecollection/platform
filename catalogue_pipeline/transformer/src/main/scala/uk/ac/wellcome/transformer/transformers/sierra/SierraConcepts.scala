package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.transformer.source.{
  MarcSubfield,
  SierraBibData,
  VarField
}

trait SierraConcepts extends MarcUtils {

  // Get the label.  This is populated by the label of subfield $a, followed
  // by other subfields, in the order they come from MARC.  The labels are
  // joined by " - ".
  protected def getLabel(primarySubfields: List[MarcSubfield],
                         subdivisionSubfields: List[MarcSubfield]): String = {
    val orderedSubfields = primarySubfields ++ subdivisionSubfields
    orderedSubfields.map { _.content }.mkString(" - ")
  }

  // Apply an identifier to the primary concept.  We look in subfield $0
  // for the identifier value, then second indicator for the authority.
  //
  // Note that some identifiers have an identifier scheme in
  // indicator 2, but no ID.  In this case, we just ignore it.
  protected def identifyPrimaryConcept[T <: AbstractConcept](
    concept: T,
    varField: VarField): MaybeDisplayable[T] = {
    val identifierSubfields = varField.subfields.filter { _.tag == "0" }

    identifierSubfields match {
      case Seq() => Unidentifiable(agent = concept)
      case Seq(identifierSubfield) =>
        maybeAddIdentifier[T](
          concept = concept,
          varField = varField,
          identifierSubfield = identifierSubfield
        )
      case _ =>
        throw new RuntimeException(
          s"Too many identifiers fields: $identifierSubfields")
    }
  }

  protected def maybeFindIdentifier(
    varField: VarField,
    identifierSubfield: MarcSubfield): Some[SourceIdentifier] = {

  }

  // If there's exactly one subfield $0 on the VarField, add an identifier
  // if possible.
  private def maybeAddIdentifier[T <: AbstractConcept](
    concept: T,
    varField: VarField,
    identifierSubfield: MarcSubfield): MaybeDisplayable[T] = {
    val maybeSourceIdentifier = SierraConceptIdentifier.maybeFindIdentifier(
      varField = varField,
      identifierSubfield = identifierSubfield,
      ontologyType = concept.ontologyType
    )

    maybeSourceIdentifier match {
      case None => Unidentifiable(agent = concept)
      case Some(sourceIdentifier) =>
        Identifiable(
          agent = concept,
          sourceIdentifier = sourceIdentifier,
          identifiers = List(sourceIdentifier)
        )
    }
  }

  // Extract the subdivisions, which come from everything except subfield $a.
  // These are never identified.  We preserve the order from MARC.
  protected def getSubdivisions(subdivisionSubfields: List[MarcSubfield])
    : List[Unidentifiable[AbstractConcept]] = {
    val concepts: List[AbstractConcept] = subdivisionSubfields.map {
      subfield =>
        subfield.tag match {
          case "v" | "x" => Concept(label = subfield.content)
          case "y" => Period(label = subfield.content)
          case "z" => Place(label = subfield.content)
        }
    }

    concepts.map { Unidentifiable(_) }
  }
}
