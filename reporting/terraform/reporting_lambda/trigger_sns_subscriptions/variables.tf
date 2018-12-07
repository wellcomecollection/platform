variable "lambda_function_name" {
  description = "Name of the Lambda function to be triggered"
}

variable "aws_region" {
  description = "AWS region to create queue in"
}

variable "account_id" {
  description = "AWS account id for account to create queue in"
}

variable "topic_count" {
  default = "1"
}

variable "topic_names" {
  type        = "list"
  description = "Topic name for the SNS topic to subscribe the queue to"
}
