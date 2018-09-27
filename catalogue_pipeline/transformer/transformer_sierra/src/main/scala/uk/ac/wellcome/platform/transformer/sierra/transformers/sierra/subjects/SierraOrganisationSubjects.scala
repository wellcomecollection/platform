package uk.ac.wellcome.platform.transformer.sierra.transformers.sierra.subjects

import uk.ac.wellcome.models.work.internal.{MaybeDisplayable, Organisation, Subject}
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
      val label = createLabel(varField)

      Subject(
        label = label,
        concepts = List()
      )
    }

  private def createLabel(varField: VarField): String =
    varField
      .subfields
      .filter { vf =>
        vf.tag == "a" || vf.tag == "b" || vf.tag == "c" || vf.tag == "d" || vf.tag == "e"
      }
      .map { _.content }
      .mkString(" ")
}
