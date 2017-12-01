variable "resource_type" {}

variable "dynamo_events_topic_name" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "account_id" {}

variable "dlq_alarm_arn" {}
