output "arn" {
  value = "${aws_lambda_function.lambda_trigger_bag_ingest_monitoring.arn}"
}

output "name" {
  value = "${aws_lambda_function.lambda_trigger_bag_ingest_monitoring.function_name}"
}

output "role_name" {
  value = "${module.lambda_trigger_bag_ingest_iam.role_name}"
}
