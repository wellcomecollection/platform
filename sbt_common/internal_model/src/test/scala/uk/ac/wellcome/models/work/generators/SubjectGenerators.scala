package uk.ac.wellcome.models.work.generators

import uk.ac.wellcome.models.generators.RandomStrings
import uk.ac.wellcome.models.work.internal._

trait SubjectGenerators extends RandomStrings {
  def createSubjectWith(
    concepts: List[Displayable[AbstractRootConcept]] = createConcepts
  ): Displayable[Subject[Displayable[AbstractRootConcept]]] =
    Unidentifiable(
      Subject(
        label = randomAlphanumeric(10),
        concepts = concepts
      )
    )

  def createSubject: Displayable[Subject[Displayable[AbstractRootConcept]]] =
    createSubjectWith()

  private def createConcepts: List[Displayable[AbstractRootConcept]] =
    (1 to 3)
      .map { _ =>
        Unidentifiable(Concept(randomAlphanumeric(15)))
      }
      .toList
      .asInstanceOf[List[Displayable[AbstractRootConcept]]]
}
