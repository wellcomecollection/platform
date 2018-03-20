variable "lambda_error_alarm_arn" {}

variable "lambda_pushes_topic_name" {}
variable "ecr_pushes_topic_name" {}

variable "slack_webhook" {
  description = "Incoming Webhook URL to send Slack notifications"
}

variable "infra_bucket" {}
