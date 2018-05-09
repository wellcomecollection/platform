package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData, VarField}

trait SierraConcepts extends MarcUtils {

  // Get the label.  This is populated by the label of subfield $a, followed
  // by other subfields, in the order they come from MARC.  The labels are
  // joined by " - ".
  protected def getLabel(primarySubfields: List[MarcSubfield], subdivisionSubfields: List[MarcSubfield]): String = {
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

    identifierSubfields.size match {
      case 0 => Unidentifiable(agent = concept)
      case 1 => {

        // The mapping from indicator 2 to the identifier scheme is provided
        // by the MARC spec.
        // https://www.loc.gov/marc/bibliographic/bd655.html
        val maybeIdentifierScheme = varField.indicator2 match {
          case None => None
          case Some("0") => Some(IdentifierSchemes.libraryOfCongressSubjectHeadings)
          case Some("2") => Some(IdentifierSchemes.medicalSubjectHeadings)
          case Some(scheme) => throw new RuntimeException(s"Unrecognised identifier scheme: $scheme")
        }

        maybeIdentifierScheme match {
          case None => Unidentifiable(agent = concept)
          case Some(identifierScheme) => {
            val sourceIdentifier = SourceIdentifier(
              identifierScheme = identifierScheme,
              value = identifierSubfields.head.content,
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

      case _ => throw new RuntimeException(s"Too many identifiers fields: $identifierSubfields")
    }
  }

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
