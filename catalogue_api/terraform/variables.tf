variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "az_count" {
  description = "Number of AZs to cover in a given AWS region"
  default     = "2"
}

variable "key_name" {
  description = "Name of AWS key pair"
}

variable "release_ids" {
  description = "Release tags for platform apps"
  type        = "map"
}

variable "api_prod_host" {
  description = "Hostname to use for the production API"
  default     = "api.wellcomecollection.org"
}

variable "api_stage_host" {
  description = "Hostname to use for the production API"
  default     = "api-stage.wellcomecollection.org"
}

variable "es_cluster_credentials" {
  description = "Credentials for the Elasticsearch cluster"
  type        = "map"
}

variable "infra_bucket" {}

variable "es_config_romulus" {
  description = "Elasticcloud config for romulus"
  type        = "map"

  default = {
    index_v1 = "v1-2018-09-27-marc-610-subjects"
    index_v2 = "v2-2018-09-27-marc-610-subjects"
    doc_type = "work"
  }
}

variable "es_config_remus" {
  description = "Elasticcloud config for remus"
  type        = "map"

  default = {
    index_v1 = "v1-2018-09-14-miro-sierra-merging-take-5"
    index_v2 = "v2-2018-09-14-miro-sierra-merging-take-5"
    doc_type = "work"
  }
}
