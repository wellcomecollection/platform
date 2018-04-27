package uk.ac.wellcome.models.work.internal

case class Contributor[+T <: IdentityState[AbstractAgent]](
  agent: T,
  roles: List[ContributionRole] = List(),
  ontologyType: String = "Contributor"
)
