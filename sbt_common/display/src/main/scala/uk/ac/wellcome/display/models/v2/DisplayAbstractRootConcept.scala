package uk.ac.wellcome.display.models.v2

import io.swagger.annotations.ApiModel
import uk.ac.wellcome.models.work.internal._

@ApiModel(
  value = "Concept"
)
trait DisplayAbstractRootConcept {
  val id: Option[String]
  val identifiers: Option[List[DisplayIdentifierV2]]
  val label: String
}

object DisplayAbstractRootConcept {
  def apply(abstractConcept: Displayable[AbstractRootConcept],
            includesIdentifiers: Boolean): DisplayAbstractRootConcept =
    abstractConcept match {
      // Horribleness to circumvent Java type erasure ಠ_ಠ
      case agentConcept @ Unidentifiable(_: AbstractAgent) =>
        DisplayAbstractAgentV2(
          agentConcept.asInstanceOf[Displayable[AbstractAgent]],
          includesIdentifiers)
      case agentConcept @ Identified(_: AbstractAgent, _, _, _) =>
        DisplayAbstractAgentV2(
          agentConcept.asInstanceOf[Displayable[AbstractAgent]],
          includesIdentifiers)
      case concept @ Unidentifiable(_: AbstractConcept) =>
        DisplayAbstractConcept(
          concept.asInstanceOf[Displayable[AbstractConcept]],
          includesIdentifiers)
      case concept @ Identified(_: AbstractConcept, _, _, _) =>
        DisplayAbstractConcept(
          concept.asInstanceOf[Displayable[AbstractConcept]],
          includesIdentifiers)
    }
}
