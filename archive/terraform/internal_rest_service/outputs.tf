output "task_role_name" {
  value = "${module.task.task_role_name}"
}

output "load_balancer_arn" {
  value = "${aws_lb.progress.arn}"
}
