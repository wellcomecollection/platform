variable "namespace" {}

variable "public_subnets" {
  type    = "list"
  default = []
}

variable "private_subnets" {
  type    = "list"
  default = []
}

variable "vpc_id" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "archive_api_container_image" {}

variable "archive_api_container_port" {}

variable "certificate_domain" {}

variable "archive_ingest_sns_topic_arn" {}

variable "api_path" {}

variable "interservice_security_group_id" {}
