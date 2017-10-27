variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "release_ids" {
  description = "Release tags for Miro preprocessor"
  type        = "map"
}

variable "cluster_url" {
  description = "Cluster URL for miro inventory"
}

variable "es_passsword" {
  description = "Password for acccess to miro inventory cluster"
}

variable "es_username" {
  description = "Username for acccess to miro inventory cluster"
}

variable "tandem_vault_api_key" {
  description = "API key for access to Tandem Vault"
}