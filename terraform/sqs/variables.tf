variable "queue_name" {
  description = "Name of the SQS queue to create"
}

variable "topic_count" {
  default = "1"
}

variable "topic_names" {
  type        = "list"
  description = "Topic name for the SNS topic to subscribe the queue to"
}

variable "aws_region" {
  description = "AWS region to create queue in"
}

variable "account_id" {
  description = "AWS account id for account to create queue in"
}

variable "max_receive_count" {
  description = "Max receive count before sending to DLQ"
  default     = "4"
}

variable "alarm_topic_arn" {
  default = ""
}