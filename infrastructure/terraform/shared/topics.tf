module "lambda_error_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v19.13.2"
  name   = "lambda_error_alarm"
}

# Alarm topics

module "dlq_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v19.13.2"
  name   = "shared_dlq_alarm"
}

module "gateway_server_error_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v19.18.0"
  name   = "shared_gateway_server_error_alarm"
  cross_account_publication_ids = ["756629837203"]
}

module "terraform_apply_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v19.13.2"
  name   = "shared_terraform_apply"
}

# Shared topics for reindexing

## Reporting

module "reporting_miro_reindex_topic" {
  source                         = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v19.13.2"
  name                           = "reporting_miro_reindex_topic"
  cross_account_subscription_ids = ["269807742353"]
}

module "reporting_sierra_reindex_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v19.13.2"
  name   = "reporting_sierra_reindex_topic"
}

module "reporting_miro_inventory_reindex_topic" {
  source                         = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v19.13.2"
  name                           = "reporting_miro_inventory_reindex_topic"
  cross_account_subscription_ids = ["269807742353"]
}

## Catalogue

module "catalogue_miro_reindex_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v19.13.2"
  name   = "catalogue_miro_reindex_topic"
}

module "catalogue_sierra_reindex_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v19.13.2"
  name   = "catalogue_sierra_reindex_topic"
}

module "catalogue_sierra_items_reindex_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v19.13.2"
  name   = "catalogue_sierra_items_reindex_topic"
}

## Inference

module "inference_calm_reindex_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v19.13.2"
  name   = "inference_calm_reindex_topic"
}

# Shared topics for updates to VHS source data
module "miro_updates_topic" {
  source                         = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v19.13.2"
  name                           = "vhs_sourcedata_miro_updates_topic"
  cross_account_subscription_ids = ["269807742353"]
}
