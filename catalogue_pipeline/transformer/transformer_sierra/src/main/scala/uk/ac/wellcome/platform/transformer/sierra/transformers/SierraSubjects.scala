package uk.ac.wellcome.platform.transformer.sierra.transformers

import uk.ac.wellcome.models.work.internal.{
  AbstractRootConcept,
  MaybeDisplayable,
  Subject
}
import uk.ac.wellcome.platform.transformer.sierra.source.SierraBibData
import uk.ac.wellcome.platform.transformer.sierra.transformers.subjects.{
  SierraConceptSubjects,
  SierraOrganisationSubjects,
  SierraPersonSubjects
}

trait SierraSubjects
    extends SierraConceptSubjects
    with SierraPersonSubjects
    with SierraOrganisationSubjects {
  def getSubjects(bibData: SierraBibData)
    : List[MaybeDisplayable[Subject[MaybeDisplayable[AbstractRootConcept]]]] =
    getSubjectswithAbstractConcepts(bibData) ++
      getSubjectsWithPerson(bibData) ++
      getSubjectsWithOrganisation(bibData)
}
