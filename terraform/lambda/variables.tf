variable "name" {
  description = "Name of the Lambda"
}

variable "description" {
  description = "Description of the Lambda function"
}

variable "filename" {
  description = "Path to the Python file containing the Lambda source code"
}

variable "sns_trigger_arn" {
  description = "If this Lambda is triggered by SNS, the ARN of the trigger topic"
  default     = ""
}

variable "cloudwatch_trigger_arn" {
  description = "If this Lambda is triggered by CloudWatch events, the ARN of the CloudWatch rule"
  default     = ""
}
