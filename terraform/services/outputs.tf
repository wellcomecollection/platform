output "service_name" {
  value = "${var.name}"
}

output "target_group_arn" {
  value = "${module.service.target_group_arn}"
}

output "role_name" {
  value = "${module.service.role_name}"
}

output "host_name" {
  value = "${var.host_name}"
}

output "config_key" {
  value = "${var.config_key}"
}
