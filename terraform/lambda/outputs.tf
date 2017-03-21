output "arn" {
  value = "${aws_lambda_function.lambda_function.arn}"
}

output "function_name" {
  value = "${aws_lambda_function.lambda_function.function_name}"
}

output "cloudwatch_arn" {
  description = "ARN of the CloudWatch log group for this Lambda"
  value       = "${aws_cloudwatch_log_group.cloudwatch_log_group.arn}"
}

output "role_arn" {
  description = "ARN of the IAM role for this Lambda"
  value       = "${aws_iam_role.iam_role.arn}"
}

output "role_name" {
  description = "Name of the IAM role for this Lambda"
  value       = "${aws_iam_role.iam_role.name}"
}
