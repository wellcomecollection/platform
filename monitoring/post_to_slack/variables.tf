variable "bitly_access_token" {}
variable "lambda_error_alarm_arn" {}
variable "dlq_alarm_arn" {}
variable "terminal_failure_alarm_arn" {}
variable "alb_server_error_alarm_arn" {}
variable "ec2_instance_terminating_for_too_long_alarm_arn" {}

variable "critical_slack_webhook" {
  description = "Incoming Webhook URL to send critical Slack notifications"
}

variable "non_critical_slack_webhook" {
  description = "Incoming Webhook URL to send non-critical Slack notifications"
}

variable "infra_bucket" {}
