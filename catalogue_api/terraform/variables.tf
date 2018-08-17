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

# These variables will change fairly regularly, whenever we want to swap the
# staging and production APIs.

variable "production_api" {
  description = "Which version of the API is production? (romulus | remus)"
  default     = "romulus"
}

variable "pinned_romulus_api" {
  description = "Which version of the API image to pin romulus to, if any"
  default     = "304a9db0d4db377b953b040386a8cafa9d912d9f"
}

variable "pinned_romulus_api_nginx-delta" {
  description = "Which version of the nginx API image to pin romulus to, if any"
  default     = "3dd8a423123e1d175dd44520fcf03435a5fc92c8"
}

variable "pinned_remus_api" {
  description = "Which version of the API image to pin remus to, if any"
  default     = ""
}

variable "pinned_remus_api_nginx-delta" {
  description = "Which version of the nginx API image to pin remus to, if any"
  default     = ""
}
