package uk.ac.wellcome.platform.transformer.sierra.transformers.sierra.subjects

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.exceptions.TransformerException
import uk.ac.wellcome.platform.transformer.sierra.source.{
  SierraBibData,
  VarField
}
import uk.ac.wellcome.platform.transformer.sierra.transformers.sierra.{
  MarcUtils,
  SierraAgents
}

trait SierraOrganisationSubjects extends SierraAgents with MarcUtils {

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
  def getSubjectsWithOrganisation(
    bibData: SierraBibData): List[Subject[MaybeDisplayable[Organisation]]] =
    getMatchingVarFields(bibData, marcTag = "610").map { varField =>
      val label =
        createLabel(varField, subfieldTags = List("a", "b", "c", "d", "e"))

      val organisation = createOrganisation(varField)

      Subject(
        label = label,
        concepts = List(organisation)
      )
    }

  private def createOrganisation(
    varField: VarField): MaybeDisplayable[Organisation] = {
    val label = createLabel(varField, subfieldTags = List("a", "b"))

    // @@AWLC: I'm not sure if this can happen in practice -- but we don't have
    // enough information to build the Organisation, so erroring out here is
    // the best we can do for now.
    if (label == "") {
      throw TransformerException(
        s"Not enough information to build a label on $varField")
    }

    val organisation = Organisation(label = label)

    varField.indicator2 match {
      case Some("0") =>
        identify(varField.subfields, organisation, "Organisation")
      case _ => Unidentifiable(organisation)
    }
  }

  /** Given a varField and a list of subfield tags, create a label by
    * concatenating the contents of every subfield with one of the given tags.
    *
    * The order is the same as that in the original MARC.
    *
    */
  private def createLabel(varField: VarField,
                          subfieldTags: List[String]): String =
    varField.subfields
      .filter { vf =>
        subfieldTags.contains(vf.tag)
      }
      .map { _.content }
      .mkString(" ")
}
