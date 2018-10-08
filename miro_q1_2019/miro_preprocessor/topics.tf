module "topic_miro_image_to_dynamo" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "miro_image_to_dynamo"
}

module "cold_store_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "miro_cold_store"
}

module "tandem_vault_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "miro_tandem_vault"
}

module "catalogue_api_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "miro_catalogue_api"
}

module "none_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "miro_none"
}

module "s3_copy_catchall" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "s3_copy_catchall"
}
