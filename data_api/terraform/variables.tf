variable "aws_region" {
  default = "eu-west-1"
}

variable "infra_bucket" {}

variable "release_ids" {
  description = "Release tags for platform apps"
  type        = "map"
}

variable "key_name" {
  description = "Name of AWS key pair"
}

variable "data_acm_cert_arn" {
  description = "ARN for the data ssl cert"
}

variable "es_cluster_credentials" {
  description = "Credentials for the Elasticsearch cluster"
  type        = "map"
}

variable "es_config_snapshot" {
  description = "Elasticcloud config for the snapshot generator"
  type        = "map"

  default = {
    index_v1 = "v1-20180530-new-identifier-schemes"
    index_v2 = "v2-20180530-new-identifier-schemes"
    doc_type = "work"
  }
}
