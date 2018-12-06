output "target_group_arn" {
  value = "${module.service.target_group_arn}"
}

output "name" {
  value = "${module.service.name}"
}

output "task_role_name" {
  value = "${module.task.task_role_name}"
}
