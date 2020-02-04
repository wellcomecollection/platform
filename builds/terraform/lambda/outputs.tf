output "arn" {
  description = "ARN of the Lambda function"
  value       = aws_lambda_function.lambda_function.arn
}

output "function_name" {
  description = "Name of the Lambda function"
  value       = aws_lambda_function.lambda_function.function_name
}

output "role_name" {
  description = "Name of the IAM role for this Lambda"
  value       = aws_iam_role.iam_role.name
}
