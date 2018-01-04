variable "resource_type" {}

variable "window_length_minutes" {}
variable "trigger_interval_minutes" {}

variable "dlq_alarm_arn" {}
variable "lambda_error_alarm_arn" {}

variable "aws_region" {
  default = "eu-west-1"
}
variable "account_id" {}
