variable "dashboard_assumable_roles" {
  description = "Assumable roles for the ECS dashboard"
  type        = "list"
}

variable "slack_webhook" {
  description = "Incoming Webhook URL to send slack notifications"
}
