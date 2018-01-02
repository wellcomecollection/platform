variable "aws_region" {
  default = "eu-west-1"
}

variable "account_id" {}
variable "dlq_alarm_arn" {}
variable "lambda_error_alarm_arn" {}

variable "resource_type" {}

variable "window_length_minutes" {}

variable "lambda_trigger_minutes" {}
