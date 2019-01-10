variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "release_ids" {
  description = "Release tags for platform apps"
  type        = "map"
}

variable "es_cluster_credentials_v6" {
  type = "map"
}

variable "infra_bucket" {}

variable "critical_slack_webhook" {}
