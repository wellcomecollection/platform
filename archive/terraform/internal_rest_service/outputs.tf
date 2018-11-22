output "task_role_name" {
  value = "${module.task.task_role_name}"
}

output "target_group_arn" {
  value = "${data.aws_lb_target_group.tcp_target_group.arn}"
}
