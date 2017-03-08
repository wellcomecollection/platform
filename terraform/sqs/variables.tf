variable "queue_name" {
  description = "Name of the SQS queue to create"
}

variable "topic_name" {
  description = "Name of the SNS topic to subscribe the queue to"
}

variable "topic_arn" {
  description = "ARN of the SNS topic to subscribe the queue to"
}

variable "aws_region" {
  description = "AWS region to create queue in"
}

variable "account_id" {
  description = "AWS account id for account to create queue in"
}
