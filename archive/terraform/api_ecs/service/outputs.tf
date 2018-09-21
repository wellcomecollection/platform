output "target_group_arn" {
  value = "${module.service.target_group_arn}"
}

output "task_role_name" {
  value = "${module.task.task_role_name}"
}
