package uk.ac.wellcome.models

case class Contributor(
  agent: AbstractAgent,
  roles: List[ContributionRole] = List(),
  ontologyType: String = "Contributor"
)
