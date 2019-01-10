variable "aws_region" {
  default = "eu-west-1"
}

variable "key_name" {}

variable "admin_cidr_ingress" {}

variable "release_ids" {
  description = "Release tags for platform apps"
  type        = "map"
}

# Bagger
variable "bagger_mets_bucket_name" {}

variable "bagger_read_mets_from_fileshare" {}

variable "bagger_working_directory" {}

variable "bagger_drop_bucket_name" {}

variable "bagger_drop_bucket_name_mets_only" {}

variable "bagger_drop_bucket_name_errors" {}

variable "bagger_current_preservation_bucket" {}

variable "bagger_dlcs_source_bucket" {}

variable "bagger_dlcs_entry" {}

variable "bagger_dlcs_api_key" {}

variable "bagger_dlcs_api_secret" {}

variable "bagger_dlcs_customer_id" {}

variable "bagger_dlcs_space" {}

variable "bagger_dds_api_key" {}

variable "bagger_dds_api_secret" {}

variable "bagger_dds_asset_prefix" {}

variable "archive_oauth_details_enc" {}
