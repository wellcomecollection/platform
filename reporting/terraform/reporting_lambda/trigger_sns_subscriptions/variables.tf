variable "lambda_function_name" {
  description = "Name of the Lambda function to be triggered"
}

variable "topic_count" {
  default = 1
}

variable "topic_arns" {
  type        = "list"
  description = "Topic arn for the SNS topic to subscribe the queue to"
}
