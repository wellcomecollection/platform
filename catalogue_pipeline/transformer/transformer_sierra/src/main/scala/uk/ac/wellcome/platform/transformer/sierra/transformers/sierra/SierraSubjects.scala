package uk.ac.wellcome.platform.transformer.sierra.transformers.sierra

import uk.ac.wellcome.models.work.internal.{AbstractRootConcept, MaybeDisplayable, Subject}
import uk.ac.wellcome.platform.transformer.sierra.source.SierraBibData
import uk.ac.wellcome.platform.transformer.sierra.transformers.sierra.subjects.{SierraConceptSubjects, SierraPersonSubjects}

trait SierraSubjects extends SierraConceptSubjects with SierraPersonSubjects {
  def getSubjects(bibData: SierraBibData): List[Subject[MaybeDisplayable[AbstractRootConcept]]] =
    getSubjectswithAbstractConcepts(bibData) ++
      getSubjectsWithPerson(bibData)
}
