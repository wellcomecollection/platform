package uk.ac.wellcome.models

case class Contributor(
  id: String,
  agent: AbstractAgent,
  roles: List[ContributionRole] = List(),
  ontologyType: String = "Contributor"
)
