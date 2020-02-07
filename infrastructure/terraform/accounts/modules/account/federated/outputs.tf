output "principal" {
  value = "${local.principal}"
}

output "list_roles_user_id" {
  value = "${module.list_roles_user.user_id}"
}

output "list_roles_user_secret" {
  value = "${module.list_roles_user.user_secret}"
}

output "list_roles_name" {
  value = "${module.list_roles_user.name}"
}

output "list_roles_arn" {
  value = "${module.list_roles_user.arn}"
}

variable "saml_xml" {}
