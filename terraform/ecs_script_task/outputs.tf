output "task_arn" {
  value = "${aws_ecs_task_definition.task.arn}"
}

output "container_name" {
  value = "${var.name}"
}
