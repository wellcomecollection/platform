output "role_name" {
  description = "Name of the IAM role for this Lambda"
  value       = "${module.reporting_lambda.role_name}"
}
