output "ingests_name" {
  value = "${module.services.ingests_name}"
}

output "bags_name" {
  value = "${module.services.bags_name}"
}

output "api_gateway_id" {
  value = "${aws_api_gateway_rest_api.api.id}"
}
