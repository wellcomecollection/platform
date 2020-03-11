variable "namespace" {}

variable "non_critical_slack_webhook" {}

variable "namespace_id" {}
variable "vpc_id" {}

variable "efs_id" {}
variable "efs_security_group_id" {}

variable "domain" {}

variable "public_subnets" {
  type = "list"
}

variable "private_subnets" {
  type = "list"
}

variable "infra_bucket" {}
variable "key_name" {}
variable "aws_region" {}
variable "admin_cidr_ingress" {}

variable "lambda_error_alarm_arn" {}

# Grafana

variable "grafana_admin_user" {}
variable "grafana_anonymous_role" {}
variable "grafana_admin_password" {}
variable "grafana_anonymous_enabled" {}

# post_to_slack

variable "dlq_alarm_arn" {}
variable "gateway_server_error_alarm_arn" {}
variable "cloudfront_errors_topic_arn" {}
variable "critical_slack_webhook" {}
variable "bitly_access_token" {}

# IAM

variable "allow_cloudwatch_read_metrics_policy_json" {}
variable "cloudwatch_allow_filterlogs_policy_json" {}
