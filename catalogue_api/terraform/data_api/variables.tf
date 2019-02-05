variable "aws_region" {
  default = "eu-west-1"
}

variable "infra_bucket" {}

variable "es_config_snapshot" {
  description = "Elasticcloud config for the snapshot generator"
  type        = "map"

  default = {
    index_v1 = "v1-2018-07-30-merging"
    index_v2 = "v2-2018-07-30-merging"
    doc_type = "work"
  }
}

variable "namespace" {
  default = "data_api"
}

variable "snapshot_generator_release_uri" {}

variable "critical_slack_webhook" {}

variable "vpc_id" {}

variable "private_subnets" {
  type = "list"
}
