output "task_arn" {
  value = "${aws_ecs_task_definition.task.arn}"
}
