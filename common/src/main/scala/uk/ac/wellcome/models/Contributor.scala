package uk.ac.wellcome.models

case class Contributor (agent: IdentityState[AbstractAgent], roles: List[ContributionRole])
