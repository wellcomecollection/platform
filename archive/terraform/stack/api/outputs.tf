output "ingests_role_name" {
  value = "${module.services.ingests_role_name}"
}

output "bags_role_name" {
  value = "${module.services.bags_role_name}"
}

output "api_gateway_id" {
  value = "${aws_api_gateway_rest_api.api.id}"
}
