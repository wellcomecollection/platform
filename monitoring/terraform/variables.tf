variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "critical_slack_webhook" {
  description = "Incoming Webhook URL to send critical Slack notifications"
}

variable "non_critical_slack_webhook" {
  description = "Incoming Webhook URL to send non-critical Slack notifications"
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

variable "key_name" {
  description = "Name of AWS key pair"
}

variable "admin_cidr_ingress" {
  description = "CIDR to allow tcp/22 ingress to EC2 instance"
}

variable "grafana_anonymous_enabled" {
  description = "Enable grafana anonymous access"
  default     = "true"
}

variable "grafana_anonymous_role" {
  description = "Specify role for anonymous users. Valid values are Viewer, Editor and Admin"
  default     = "Editor"
}

variable "grafana_admin_user" {
  description = "The name of the default Grafana admin user"
  default     = "admin"
}

variable "grafana_admin_password" {
  description = "The password of the default Grafana admin"
}

variable "infra_bucket" {}
