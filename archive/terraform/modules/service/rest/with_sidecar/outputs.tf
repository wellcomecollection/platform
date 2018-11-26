output "target_group_name" {
  value = "${module.rest_service_with_sidecar.target_group_name}"
}

output "task_role_name" {
  value = "${module.rest_service_with_sidecar.task_role_name}"
}
