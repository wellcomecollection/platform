package uk.ac.wellcome.platform.transformer.sierra.transformers.sierra.subjects

import uk.ac.wellcome.models.work.internal.{MaybeDisplayable, Organisation, Subject, Unidentifiable}
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
  def getSubjectsWithOrganisation(bibData: SierraBibData): List[Subject[MaybeDisplayable[Organisation]]] =
    getMatchingVarFields(bibData, marcTag = "610").map { varField =>
      val label = createLabel(varField, subfieldTags = List("a", "b", "c", "d", "e"))

      val organisation = createOrganisation(varField)

      Subject(
        label = label,
        concepts = List(organisation)
      )
    }

  private def createOrganisation(varField: VarField): MaybeDisplayable[Organisation] =
    Unidentifiable(
      Organisation(label = "ACME Corp")
    )

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
