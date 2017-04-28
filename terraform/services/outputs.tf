output "service_name" {
  value = "${var.name}"
}

output "target_group_arn" {
  value = "${module.service.target_group_arn}"
}

output "role_name" {
  value = "${module.service.role_name}"
}
