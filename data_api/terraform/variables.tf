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
    index_v1 = "v1-2018-06-05-new-location-types"
    index_v2 = "v2-2018-06-05-new-location-types"
    doc_type = "work"
  }
}
