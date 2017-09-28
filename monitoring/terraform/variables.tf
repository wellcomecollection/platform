variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "slack_webhook" {
  description = "Incoming Webhook URL to send slack notifications"
}

variable "bitly_access_token" {
  description = "Access token for the bit.ly API"
}

variable "dashboard_assumable_roles" {
  description = "Assumable roles for the ECS dashboard"
  type        = "list"
}

variable "release_ids" {
  description = "Release tags for platform apps"
  type        = "map"
}

variable "dash_bucket" {
  description = "S3 bucket hosting our dashboard"
  default     = "wellcome-platform-dash"
}
