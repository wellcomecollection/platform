package uk.ac.wellcome.platform.transformer.sierra.transformers.sierra.subjects

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.source.{SierraBibData, VarField}
import uk.ac.wellcome.platform.transformer.sierra.transformers.sierra.MarcUtils

trait SierraOrganisationSubjects extends MarcUtils {

  // Populate wwork:subject
  //
  // Use MARC field "610".
  //
  // *  Populate the platform "label" with the concatenated values of
  //    subfields a, b, c, d and e.
  //
  // *  Populate "concepts" with a single value:
  //
  //    -   Create "label" from subfields a and b
  //    -   Set "type" to "Organisation"
  //    -   Use subfield 0 to populate "identifiers", if present.  Note the
  //        identifierType should be "lc-names".
  //
  // https://www.loc.gov/marc/bibliographic/bd610.html
  //
  def getSubjectsWithOrganisation(bibData: SierraBibData): List[Subject[MaybeDisplayable[Organisation]]] =
    getMatchingVarFields(bibData, marcTag = "610").map { varField =>
      val label = createLabel(varField, subfieldTags = List("a", "b", "c", "d", "e"))

      val organisation = createOrganisation(varField)

      Subject(
        label = label,
        concepts = List(organisation)
      )
    }

  private def createOrganisation(varField: VarField): MaybeDisplayable[Organisation] = {
    val label = createLabel(varField, subfieldTags = List("a", "b"))
    val organisation = Organisation(label = label)

    val identifierSubfields = varField.subfields.filter { _.tag == "0" }

    identifierSubfields match {
      case Seq(subfield) => {
        val sourceIdentifier = SourceIdentifier(
          identifierType = IdentifierType("lc-names"),
          value = subfield.content,
          ontologyType = "Organisation"
        )

        Identifiable(
          agent = organisation,
          sourceIdentifier = sourceIdentifier
        )
      }
      case _ => Unidentifiable(organisation)
    }
  }

  /** Given a varField and a list of subfield tags, create a label by
    * concatenating the contents of every subfield with one of the given tags.
    *
    * The order is the same as that in the original MARC.
    *
    */
  private def createLabel(varField: VarField, subfieldTags: List[String]): String =
    varField
      .subfields
      .filter { vf => subfieldTags.contains(vf.tag) }
      .map { _.content }
      .mkString(" ")
}