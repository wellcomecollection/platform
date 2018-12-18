variable "vpc_id" {}
variable "namespace" {}
variable "aws_region" {}

variable "private_subnets" {
  type = "list"
}

variable "public_subnets" {
  type = "list"
}

variable "key_name" {}
variable "certificate_domain" {}

variable "healthcheck_path" {
  default = "/image/"
}

variable "ebs_container_path" {
  default = "/mnt/loris"
}

variable "ebs_volume_size" {
  default = "180"
}

variable "log_group_prefix" {
  description = "Cloudwatch log group name prefix"
  default     = "ecs"
}

variable "asg_min" {
  description = "Min number of instances"
  default     = "0"
}

variable "asg_desired" {
  description = "Desired number of instances"
  default     = "4"
}

variable "asg_max" {
  description = "Max number of instances"
  default     = "4"
}

variable "cpu" {
  default = "3960"
}

variable "memory" {
  default = "7350"
}

variable "task_desired_count" {
  default = "4"
}

variable "instance_type" {
  default = "c4.xlarge"
}

variable "app_container_image" {}

variable "app_container_port" {
  default = "8888"
}

variable "app_cpu" {
  default = "2948"
}

variable "app_memory" {
  default = "6144"
}

variable "app_env_vars" {
  description = "Environment variables to pass to the container"
  type        = "map"
  default     = {}
}

variable "sidecar_container_image" {}

variable "sidecar_container_port" {
  default = "9000"
}

variable "sidecar_cpu" {
  default = "128"
}

variable "sidecar_memory" {
  default = "128"
}

variable "sidecar_env_vars" {
  description = "Environment variables to pass to the container"
  type        = "map"
  default     = {}
}

variable "ebs_size" {
  default = "180"
}

variable "ebs_volume_type" {
  default = "gp2"
}

variable "ebs_cache_cleaner_daemon_cpu" {
  default = "128"
}

variable "ebs_cache_cleaner_daemon_memory" {
  default = "64"
}

variable "ebs_cache_cleaner_daemon_max_age_in_days" {
  default = "30"
}

variable "ebs_cache_cleaner_daemon_max_size_in_gb" {
  default = "160"
}

variable "ebs_cache_cleaner_daemon_clean_interval" {
  default = "10m"
}

variable "ebs_cache_cleaner_daemon_image_version" {
  default = "latest"
}
