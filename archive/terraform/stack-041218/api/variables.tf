variable "subnets" {
  type = "list"
}

variable "aws_region" {
  default = "eu-west-1"
}

variable "vpc_id" {}

variable "cluster_id" {}

variable "namespace" {}
variable "namespace_id" {}

variable "bags_container_image" {}
variable "bags_container_port" {}

variable "bags_env_vars" {
  type = "map"
}

variable "bags_env_vars_length" {}
variable "bags_nginx_container_image" {}
variable "bags_nginx_container_port" {}

variable "ingests_container_image" {}
variable "ingests_container_port" {}

variable "ingests_env_vars" {
  type = "map"
}

variable "ingests_env_vars_length" {}
variable "ingests_nginx_container_port" {}
variable "ingests_nginx_container_image" {}

variable "cognito_user_pool_arn" {}

variable "auth_scopes" {
  type = "list"
}

variable "alarm_topic_arn" {}

variable "storage_static_content_bucket_name" {}
variable "interservice_security_group_id" {}

variable "domain_name" {}
