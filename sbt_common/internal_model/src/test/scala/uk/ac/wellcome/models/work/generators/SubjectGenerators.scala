package uk.ac.wellcome.models.work.generators

import uk.ac.wellcome.models.generators.RandomStrings
import uk.ac.wellcome.models.work.internal._

trait SubjectGenerators extends RandomStrings {
  def createSubjectWith(
    concepts: List[Displayable[AbstractRootConcept]] = createConcepts
  ): Subject[Displayable[AbstractRootConcept]] =
    Subject(
      label = randomAlphanumeric(10),
      concepts = concepts
    )

  def createSubject: Subject[Displayable[AbstractRootConcept]] =
    createSubjectWith()

  def createSubjectList(count: Int = 3): List[Subject[Displayable[AbstractRootConcept]]] =
    (1 to count).map { _ => createSubject }.toList

  private def createConcepts: List[Displayable[AbstractRootConcept]] =
    (1 to 3)
      .map { _ => Unidentifiable(Concept(randomAlphanumeric(15))) }
      .asInstanceOf[List[Displayable[AbstractRootConcept]]]
}
