output "dynamodb_table_deployments_arn" {
  value = "${aws_dynamodb_table.deployments.arn}"
}

output "dynamodb_table_deployments_name" {
  value = "${aws_dynamodb_table.deployments.name}"
}
