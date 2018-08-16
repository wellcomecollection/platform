package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.exceptions.TransformerException
import uk.ac.wellcome.platform.transformer.source.MarcSubfield

trait SierraAgents {

  def getPerson(subfields: List[MarcSubfield]) = {
    val label = getLabel(subfields)

    // Extract the numeration from subfield $b.  This is also non-repeatable
    // in the MARC spec.
    val numeration = subfields.collectFirst {
      case MarcSubfield("b", content) => content
    }

    // Extract the prefix from subfield $c.  This is a repeatable field, so
    // we take all instances and join them.
    val prefixes = subfields.collect {
      case MarcSubfield("c", content) => content
    }
    val prefixString =
      if (prefixes.isEmpty) None else Some(prefixes.mkString(" "))

    Person(
      label = label,
      prefix = prefixString,
      numeration = numeration
    )
  }

  def getOrganisation(subfields: List[MarcSubfield]) = {
    val label = getLabel(subfields)
    Organisation(label = label)
  }

  /* Given an agent and the associated MARC subfields, look for instances of subfield $0,
   * which are used for identifiers.
   *
   * This methods them (if present) and wraps the agent in Unidentifiable or Identifiable
   * as appropriate.
   */
  def identify[T](subfields: List[MarcSubfield],
                  agent: T,
                  ontologyType: String): MaybeDisplayable[T] = {

    // We take the contents of subfield $0.  They may contain inconsistent
    // spacing and punctuation, such as:
    //
    //    " nr 82270463"
    //    "nr 82270463"
    //    "nr 82270463.,"
    //
    // which all refer to the same identifier.
    //
    // For consistency, we remove all whitespace and some punctuation
    // before continuing.
    val codes = subfields.collect {
      case MarcSubfield("0", content) => content.replaceAll("[.,\\s]", "")
    }

    // If we get exactly one value, we can use it to identify the record.
    // Some records have multiple instances of subfield $0 (it's a repeatable
    // field in the MARC spec).
    codes.distinct match {
      case Seq(code) => {
        val sourceIdentifier = SourceIdentifier(
          identifierType = IdentifierType("lc-names"),
          value = code,
          ontologyType = ontologyType
        )
        Identifiable(
          agent = agent,
          sourceIdentifier = sourceIdentifier
        )
      }
      case _ => Unidentifiable(agent)
    }
  }

  private def getLabel(subfields: List[MarcSubfield]): String = {
    // Extract the label from subfield $a.  This is a non-repeatable
    // field in the MARC spec, but we have seen records where it
    // doesn't appear.
    val maybeSubfieldA = subfields.collectFirst {
      case MarcSubfield("a", content) => content
    }

    maybeSubfieldA match {
      case Some(content) => content
      case None =>
        throw TransformerException(
          s"Unable to find subfield $$a? <<$subfields>>")
    }
  }
}
