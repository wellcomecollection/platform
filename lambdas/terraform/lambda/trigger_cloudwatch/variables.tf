variable "lambda_function_name" {
  description = "Name of the Lambda function to be triggered"
}

variable "lambda_function_arn" {
  description = "ARN of the Lambda function to be triggered"
}

variable "cloudwatch_trigger_arn" {
  description = "ARN of the CloudWatch event"
}

variable "cloudwatch_trigger_name" {
  description = "Name of the CloudWatch event"
}

variable "input" {
  description = "Input to the Cloudwatch trigger"
  default     = "{}"
}

variable "custom_input" {
  description = "Use custom input or match event"
  default     = true
}
