variable "lambda_error_alarm_arn" {}

variable "terraform_apply_topic_name" {}

variable "slack_webhook" {
  description = "Incoming Webhook URL to send Slack notifications"
}

variable "bitly_access_token" {}

variable "monitoring_bucket" {}

variable "infra_bucket" {}
