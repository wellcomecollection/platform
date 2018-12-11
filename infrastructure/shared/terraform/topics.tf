module "lambda_error_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "lambda_error_alarm"
}

# Shared topic for terminating instances

module "ec2_terminating_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "shared_ec2_terminating_topic"
}

# Alarm topics

module "dlq_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "shared_dlq_alarm"
}

module "gateway_server_error_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "shared_gateway_server_error_alarm"
}

module "terraform_apply_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "shared_terraform_apply"
}

# Shared topics for reindexing

## Reporting

module "reporting_miro_reindex_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "reporting_miro_reindex_topic"
}

module "reporting_sierra_reindex_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "reporting_sierra_reindex_topic"
}

module "reporting_miro_inventory_reindex_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "reporting_miro_inventory_reindex_topic"
}

## Catalogue

module "catalogue_miro_reindex_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "catalogue_miro_reindex_topic"
}

module "catalogue_sierra_reindex_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "catalogue_sierra_reindex_topic"
}

module "catalogue_sierra_items_reindex_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "catalogue_sierra_items_reindex_topic"
}

# Shared topics for updates to VHS source data
module "miro_updates_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "vhs_sourcedata_miro_updates_topic"
}
