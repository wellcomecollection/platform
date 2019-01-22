package uk.ac.wellcome.platform.transformer.sierra.transformers

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.source.MarcSubfield

trait SierraAgents {
  // This is used to construct a Person from MARc tags 100, 700 and 600.
  // For all these cases:
  //  - subfield $a populates the person label
  //  - subfield $b populates the person numeration
  //  - subfield $c populates the person prefixes
  //
  def getPerson(subfields: List[MarcSubfield],
                normalisePerson: Boolean = false): Option[Person] =
    getLabel(subfields).map { label =>
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

      // The rule is to only normalise the 'Person' label when a contributor.  Strictly a 'Person' within
      // 'Subjects' (sourced from Marc 600) should not be normalised -- however, as these labels
      // are not expected to have punctuation normalisation should not change the 'Person' label for 'Subjects'
      // In which case normalisation is effectively a no-op and the test can be removed and Person.normalised
      // always returned when confident in the data.
      if (normalisePerson) {
        Person.normalised(
          label = label,
          prefix = prefixString,
          numeration = numeration
        )
      } else {
        Person(
          label = label,
          prefix = prefixString,
          numeration = numeration
        )
      }
    }

  // This is used to construct an Organisation from MARC tags 110 and 710.
  // For all entries:
  //  - Subfield $a is "label"
  //  - Subfield $0 is used to populate "identifiers". The identifier scheme is lc-names.
  //
  def getOrganisation(subfields: List[MarcSubfield]): Option[Organisation] =
    getLabel(subfields).map { label =>
      Organisation.normalised(label = label)
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

  def getLabel(subfields: List[MarcSubfield]): Option[String] =
    // Extract the label from subfield $a.  This is a non-repeatable
    // field in the MARC spec, but we have seen records where it
    // doesn't appear.  In that case, we discard the entire agent.
    subfields.collectFirst {
      case MarcSubfield("a", content) => content
    }
}
