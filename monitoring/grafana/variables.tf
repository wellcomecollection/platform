variable "ecs_monitoring_iam_instance_role_name" {}
variable "cluster_id" {}
variable "vpc_id" {}
variable "listener_https_arn" {}
variable "listener_http_arn" {}

variable "release_ids" {
  type = "map"
}

variable "efs_mount_directory" {}
variable "grafana_anonymous_enabled" {}
variable "grafana_anonymous_role" {}
variable "grafana_admin_user" {}
variable "grafana_admin_password" {}
variable "cloudwatch_id" {}
variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}
