# We can run two versions of the API: a "production" and a "staging" API.
# The idea is that we can run the new version of the API behind a different
# hostname, then promote it to production when it's ready to go.
#
# This file contains all the different bits of config for the two versions,
# followed by heavy abuse of Terraform's ternary operator in services.tf.
#
# https://www.terraform.io/docs/configuration/interpolation.html#conditionals

# These variables will change fairly regularly, whenever we want to swap the
# staging and production APIs.

variable "production_api" {
  description = "Which version of the API is production? (romulus | remus)"
  default     = "romulus"
}

variable "pinned_romulus_api" {
  description = "Which version of the API image to pin romulus to, if any"
  default     = "06ec423fc0233ede77f7309f56f3ba598f4f49d0_dev"
}

variable "pinned_romulus_api_nginx" {
  description = "Which version of the nginx API image to pin romulus to, if any"
  default     = "1e1e03187d78c1a747ef6917ca65f45e486e3240"
}

variable "pinned_remus_api" {
  description = "Which version of the API image to pin remus to, if any"
  default     = ""
}

variable "pinned_remus_api_nginx" {
  description = "Which version of the nginx API image to pin remus to, if any"
  default     = ""
}

variable "api_task_count_stage" {
  description = "How many tasks to run in the staging API"
  default     = 1
}

# These variables change less frequently -- the service blocks in services.tf
# will choose which variable to use based on the value of `production_api`.

variable "api_task_count" {
  description = "How many tasks to run in the production API"
  default     = 3
}

variable "api_host" {
  description = "Hostname to use for the production API"
  default     = "api.wellcomecollection.org"
}

variable "api_host_stage" {
  description = "Hostname to use for the staging version of the API"
  default     = "api-stage.wellcomecollection.org"
}

variable "es_config_romulus" {
  description = "ElasticCloud config for romulus"
  type        = "map"
  default     = {}
}

variable "es_config_remus" {
  description = "ElasticCloud config for remus"
  type        = "map"
  default     = {}
}
