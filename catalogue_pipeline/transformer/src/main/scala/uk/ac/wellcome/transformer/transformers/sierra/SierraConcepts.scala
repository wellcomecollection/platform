package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.transformer.source.{MarcSubfield, VarField}

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

  // If there's exactly one subfield $0 on the VarField, add an identifier
  // if possible.
  private def maybeAddIdentifier[T <: AbstractConcept](
    concept: T,
    varField: VarField,
    identifierSubfield: MarcSubfield): MaybeDisplayable[T] = {

    // The mapping from indicator 2 to the identifier scheme is provided
    // by the MARC spec.
    // https://www.loc.gov/marc/bibliographic/bd655.html
    val maybeIdentifierScheme = varField.indicator2 match {
      case None => None
      case Some("0") =>
        Some(IdentifierSchemes.libraryOfCongressSubjectHeadings)
      case Some("2") => Some(IdentifierSchemes.medicalSubjectHeadings)
      case Some(scheme) =>
        throw new RuntimeException(s"Unrecognised identifier scheme: $scheme")
    }

    maybeIdentifierScheme match {
      case None => Unidentifiable(agent = concept)
      case Some(identifierScheme) => {
        val sourceIdentifier = SourceIdentifier(
          identifierScheme = identifierScheme,
          value = identifierSubfield.content,
          ontologyType = concept.ontologyType
        )

        Identifiable(
          agent = concept,
          sourceIdentifier = sourceIdentifier,
          identifiers = List(sourceIdentifier)
        )
      }
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
