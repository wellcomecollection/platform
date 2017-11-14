output "dynamodb_table_tasks_arn" {
  value = "${aws_dynamodb_table.tasks.arn}"
}

output "dynamodb_table_tasks_name" {
  value = "${aws_dynamodb_table.tasks.name}"
}
