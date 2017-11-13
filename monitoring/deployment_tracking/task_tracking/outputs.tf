output "dynamodb_table_tasks_arn" {
  value = "${aws_dynamodb_table.tasks.arn}"
}

output "dynamodb_table_tasks_name" {
  value = "${aws_dynamodb_table.tasks.name}"
}

output "task_updates_topic_arn" {
  value = "${module.task_updates_topic.arn}"
}
