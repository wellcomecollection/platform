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
