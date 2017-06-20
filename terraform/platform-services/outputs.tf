output "service_name" {
  value = "${var.name}"
}

output "target_group_arn" {
  value = "${module.service-tasks.target_group_arn}"
}

output "role_name" {
  value = "${module.service-tasks.role_name}"
}
