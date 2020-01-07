# Cognito

output "cognito_user_pool_arn" {
  value = "${aws_cognito_user_pool.pool.arn}"
}

output "cognito_storage_api_identifier" {
  value = "${aws_cognito_resource_server.storage_api.identifier}"
}

output "cognito_stacks_api_identifier" {
  value = "${aws_cognito_resource_server.stacks_api.identifier}"
}

# Misc

output "admin_cidr_ingress" {
  value = "${local.admin_cidr_ingress}"
}

# Elasticcloud

output "elasticcloud_access_id" {
  value = "${module.elasticcloud-catalogue.elasticcloud_access_id}"
}

output "elasticcloud_access_secret" {
  value = "${module.elasticcloud-catalogue.elasticcloud_access_secret}"
}

output "elasticcloud_readonly_role_arn" {
  value = "${module.elasticcloud-catalogue.elasticcloud_readonly_role_arn}"
}

output "elasticcloud_readonly_role_name" {
  value = "${module.elasticcloud-catalogue.elasticcloud_readonly_role_name}"
}