module "role" {
  source              = "../assumable_role/federated"
  name                = var.name
  federated_principal = var.federated_principal
  aws_principal       = var.aws_principal
}

module "role_policy" {
  source    = "../role_policies/assume_role"
  role_name = module.role.name

  assumable_roles = var.assumable_role_arns
}
