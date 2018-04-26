package uk.ac.wellcome.work_model

case class Contributor[+T <: IdentityState[AbstractAgent]](
  agent: T,
  roles: List[ContributionRole] = List(),
  ontologyType: String = "Contributor"
)
