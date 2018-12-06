variable "namespace" {}

variable "domain_name" {}

variable "lambda_error_alarm_arn" {}
variable "dlq_alarm_arn" {}

variable "infra_bucket" {}
variable "current_account_id" {}

variable "ssh_key_name" {}
variable "instance_type" {}

# IAM

variable "ingest_get_policy_json" {}

variable "archive_store_policy_json" {}
variable "archive_get_policy_json" {}

variable "vhs_archive_manifest_full_access_policy_json" {}
variable "vhs_archive_manifest_read_policy_json" {}

variable "bagger_get_policy_json" {}
variable "bagger_store_policy_json" {}
variable "bagger_get_dlcs_policy_json" {}
variable "bagger_get_preservica_policy_json" {}

# Security groups

variable "service_egress_security_group_id" {}
variable "interservice_security_group_id" {}

# Network

variable "controlled_access_cidr_ingress" {
  type = "list"
}

variable "public_subnets" {
  type = "list"
}

variable "private_subnets" {
  type = "list"
}

variable "vpc_id" {}
variable "vpc_cidr" {}

variable "aws_region" {
  default = "eu-west-1"
}

# Container images

variable "archivist_container_image" {}
variable "registrar_async_container_image" {}
variable "notifier_container_image" {}
variable "progress_async_container_image" {}
variable "bagger_container_image" {}
variable "callback_stub_server_container_image" {}
variable "registrar_http_container_image" {}

variable "nginx_container_image" {}
variable "progress_http_container_image" {}

# Configuration

variable "archive_bucket_name" {}
variable "vhs_archive_manifest_bucket_name" {}
variable "vhs_archive_manifest_table_name" {}
variable "storage_static_content_bucket_name" {}

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
variable "bagger_dds_api_secret" {}
variable "bagger_dds_api_key" {}
variable "bagger_dds_asset_prefix" {}

variable "cognito_storage_api_identifier" {}
variable "cognito_user_pool_arn" {}
