variable "timeout" {
  default = 25
}

variable "name" {}
variable "description" {}

variable "environment_variables" {
  type = "map"
}

variable "topic_names" {
  type        = "list"
  description = "Topic name for the SNS topic to subscribe the queue to"
}

variable "error_alarm_topic_arn" {}

variable "log_retention_in_days" {
  default = 7
}

variable "vhs_read_policy" {}


variable "aws_region" {
  description = "AWS region to create queue in"
}

variable "account_id" {
  description = "AWS account id for account to create queue in"
}