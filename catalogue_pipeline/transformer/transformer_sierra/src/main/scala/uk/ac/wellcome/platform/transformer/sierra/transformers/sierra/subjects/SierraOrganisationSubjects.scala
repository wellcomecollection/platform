package uk.ac.wellcome.platform.transformer.sierra.transformers.sierra.subjects

import uk.ac.wellcome.models.work.internal.{MaybeDisplayable, Organisation, Subject}
import uk.ac.wellcome.platform.transformer.sierra.source.SierraBibData

trait SierraOrganisationSubjects {

  // Populate wwork:subject
  //
  // Use MARC field "610".
  def getSubjectsWithOrganisation(bibData: SierraBibData): List[Subject[MaybeDisplayable[Organisation]]] =
    List()

}
