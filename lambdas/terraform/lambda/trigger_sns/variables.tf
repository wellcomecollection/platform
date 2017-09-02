variable "lambda_function_name" {
  description = "Name of the Lambda function to be triggered"
}

variable "lambda_function_arn" {
  description = "ARN of the Lambda function to be triggered"
}

variable "sns_trigger_arn" {
  description = "ARN of the trigger topic"
}
