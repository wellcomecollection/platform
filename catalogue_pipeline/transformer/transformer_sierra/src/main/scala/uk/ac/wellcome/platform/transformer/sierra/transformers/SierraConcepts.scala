package uk.ac.wellcome.platform.transformer.sierra.transformers

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.source.{
  MarcSubfield,
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

  /** Return a list of the distinct contents of every subfield 0 on
    * this varField, which is a commonly-used subfield for identifiers.
    */
  def getIdentifierSubfieldContents(varField: VarField): List[String] =
    varField.subfields
      .filter { _.tag == "0" }
      .map { _.content }

      // We've seen the following data in subfield $0 which needs to be
      // normalisation:
      //
      //  * The same value repeated multiple times
      //    ['D000056', 'D000056']
      //
      //  * The value repeated with the prefix (DNLM)
      //    ['D049671', '(DNLM)D049671']
      //
      //    Here the prefix is denoting the authority it came from, which is
      //    an artefact of the original Sierra import.  We don't need it.
      //
      //  * The value repeated with trailing punctuation
      //    ['D004324', 'D004324.']
      //
      //  * The value repeated with varying whitespace
      //    ['n  82105476 ', 'n 82105476']
      //
      //  * The value repeated with a MESH URL prefix
      //    ['D049671', 'https://id.nlm.nih.gov/mesh/D049671']
      //
      .map { _.replaceFirst("^\\(DNLM\\)", "") }
      .map { _.replaceFirst("^https://id\\.nlm\\.nih\\.gov/mesh/", "") }
      .map { _.replaceAll("[.\\s]", "") }
      .distinct

  // Apply an identifier to the primary concept.  We look in subfield $0
  // for the identifier value, then second indicator for the authority.
  //
  // Note that some identifiers have an identifier scheme in
  // indicator 2, but no ID.  In this case, we just ignore it.
  def identifyConcept[T](concept: T, varField: VarField): MaybeDisplayable[T] =
    getIdentifierSubfieldContents(varField) match {
      case Seq(subfieldContent) =>
        maybeAddIdentifier[T](
          concept = concept,
          varField = varField,
          identifierSubfieldContent = subfieldContent
        )
      case _ => Unidentifiable(agent = concept)
    }

  // If there's exactly one subfield $0 on the VarField, add an identifier
  // if possible.
  private def maybeAddIdentifier[T](
    concept: T,
    varField: VarField,
    identifierSubfieldContent: String): MaybeDisplayable[T] = {
    val maybeSourceIdentifier = SierraConceptIdentifier.maybeFindIdentifier(
      varField = varField,
      identifierSubfieldContent = identifierSubfieldContent,
      ontologyType = concept.getClass.getSimpleName
    )

    maybeSourceIdentifier match {
      case None => Unidentifiable(agent = concept)
      case Some(sourceIdentifier) =>
        Identifiable(
          agent = concept,
          sourceIdentifier = sourceIdentifier
        )
    }
  }

  // Extract the subdivisions, which come from everything except subfield $a.
  // These are never identified.  We preserve the order from MARC.
  protected def getSubdivisions(subdivisionSubfields: List[MarcSubfield])
    : List[Unidentifiable[AbstractConcept]] = {
    val concepts: List[AbstractConcept] = subdivisionSubfields.map { subfield =>
      subfield.tag match {
        case "v" | "x" => Concept(label = subfield.content)
        case "y"       => Period(label = subfield.content)
        case "z"       => Place(label = subfield.content)
      }
    }
    concepts.map { Unidentifiable(_) }
  }
}
