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

variable "key_name" {
  description = "Name of AWS key pair"
}

variable "admin_cidr_ingress" {
  description = "CIDR to allow tcp/22 ingress to EC2 instance"
}

variable "infra_bucket" {
  description = "S3 bucket storing our configuration"
  default     = "platform-infra"
}

variable "build_env" {
  description = "Build environment (prod, dev, stage, ...)"
  default     = "prod"
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

variable "alb_logs_id" {}
variable "ec2_terminating_topic_arn" {}
variable "ec2_instance_terminating_for_too_long_alarm_arn" {}
variable "ec2_terminating_topic_publish_policy" {}
variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}
