variable "name" {
  description = "Name of the ECS service and task to create"
}

variable "alb_priority" {
  description = "ALB listener rule priority"
  default     = "100"
}

variable "desired_count" {
  description = "Desired task count per service"
  default     = "1"
}

variable "cluster_id" {
  description = "ID of the cluster which this service should run in"
}

variable "template_name" {
  description = "Name of the template to use"
  default     = "default"
}

variable "task_role_arn" {
  description = "ARN of the task definition to run in this service"
}

variable "volume_name" {
  description = "Name of volume to mount (if required)"
  default     = "ephemera"
}

variable "volume_host_path" {
  description = "Location of mount point on host path (if required)"
  default     = "/tmp"
}

variable "primary_container_name" {
  description = "Primary container to expose for service"
  default     = "nginx"
}

variable "primary_container_port" {
  description = "Port on primary container to expose for service"
  default     = "9000"
}

variable "secondary_container_port" {
  description = "Port exposed by secondary container for access the primary container"
  default     = "8888"
}

variable "vpc_id" {
  description = "ID of VPC to run service in"
}

variable "nginx_uri" {
  description = "URI of container image for nginx"
  default     = ""
}

variable "app_uri" {
  description = "URI of container image for app"
  default     = ""
}

variable "loadbalancer_cloudwatch_id" {
  description = "LoadBalancer ARN Suffix"
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
  default     = "/management/healthcheck"
}

variable "infra_bucket" {
  description = "Name of the S3 infra bucket"
  default     = ""
}

variable "config_key" {
  description = "Location of config file within S3"
  default     = ""
}

variable "config_vars" {
  description = "Variables for the config template"
  type        = "map"
  default     = {}
}

variable "container_path" {
  description = "Path of the mounted volume in the docker container"
  default     = "/tmp"
}

variable "extra_vars" {
  description = "Environment variables to pass to the container"
  type        = "list"
  default     = []
}

variable "is_config_managed" {
  description = "Flag to tell whether the config should be generated using templates and put in S3"
  default     = true
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

variable "memory" {
  description = "How much memory to allocate to the app"
  default     = 2048
}

variable "cpu" {
  description = "How much CPU to allocate to the app"
  default     = 512
}

variable "deployment_minimum_healthy_percent" {
  default = "100"
}

variable "deployment_maximum_percent" {
  default = "200"
}

variable "enable_alb_alarm" {
  default = 1
}
