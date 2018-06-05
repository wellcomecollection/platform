# We can run two versions of the API: a "production" and a "staging" API.
# The idea is that we can run the new version of the API behind a different
# hostname, then promote it to production when it's ready to go.
#
# This file contains all the different bits of config for the two versions,
# followed by heavy abuse of Terraform's ternary operator in services.tf.
#
# https://www.terraform.io/docs/configuration/interpolation.html#conditionals

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

# These variables will change fairly regularly, whenever we want to swap the
# staging and production APIs.

variable "production_api" {
  description = "Which version of the API is production? (romulus | remus)"
  default     = "romulus"
}

variable "pinned_romulus_api" {
  description = "Which version of the API image to pin romulus to, if any"
  default     = "3709fe4bc8d54c07709f8b11e995f40ad6ca39f7"
}

variable "pinned_romulus_api_nginx" {
  description = "Which version of the nginx API image to pin romulus to, if any"
  default     = "4d0b58c7cd5feefbe77637f7fcda0d93b645e11b"
}

variable "pinned_remus_api" {
  description = "Which version of the API image to pin remus to, if any"
  default     = ""
}

variable "pinned_remus_api_nginx" {
  description = "Which version of the nginx API image to pin remus to, if any"
  default     = ""
}

# These variables change less frequently -- the service blocks in services.tf
# will choose which variable to use based on the value of `production_api`.

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

variable "es_config_romulus" {
  description = "Elasticcloud config for romulus"
  type        = "map"

  default = {
    index_v1 = "v1-20180530-new-identifier-schemes"
    index_v2 = "v2-20180530-new-identifier-schemes"
    doc_type = "work"
  }
}

variable "es_config_remus" {
  description = "Elasticcloud config for remus"
  type        = "map"

  default = {
    index_v1 = "v1-20180530-new-identifier-schemes"
    index_v2 = "v2-20180530-new-identifier-schemes"
    doc_type = "work"
  }
}
