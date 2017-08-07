variable "alb_priority" {
  description = "ALB listener rule priority"
  default     = "100"
}

variable "desired_count" {
  description = "Desired task count per service"
  default     = "1"
}

variable "service_name" {
  description = "Name of the ECS service to create"
}

variable "cluster_id" {
  description = "ID of the cluster which this service should run in"
}

variable "task_definition_arn" {
  description = "ARN of the task definition to run in this service"
}

variable "container_name" {
  description = "Primary container to expose for service"
}

variable "container_port" {
  description = "Port on primary container to expose for service"
}

variable "vpc_id" {
  description = "ID of VPC to run target_group in"
}

variable "listener_https_arn" {
  description = "ARN of listener for HTTPS listener rule"
}

variable "listener_http_arn" {
  description = "ARN of listener for HTTP listener rule"
}

variable "path_pattern" {
  description = "path pattern to match for listener rule"
  default     = "/*"
}

variable "healthcheck_path" {
  description = "path for ECS healthcheck endpoint"
}

variable "infra_bucket" {
  description = "Name of the AWS Infra bucket"
}

variable "host_name" {
  description = "Hostname to be matched in the host condition"
  default     = ""
}

variable "server_error_alarm_topic_arn" {
  description = "ARN of the topic where to send notification for 5xx ALB state"
}

variable "client_error_alarm_topic_arn" {
  description = "ARN of the topic where to send notification for 4xx ALB state"
}

variable "loadbalancer_cloudwatch_id" {
  description = "LoadBalancer ARN Suffix"
}

variable "enable_alb_alarm" {
  default = true
}

variable "deployment_minimum_healthy_percent" {}
variable "deployment_maximum_percent" {}
