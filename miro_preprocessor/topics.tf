module "topic_miro_image_to_dynamo" {
  source = "../terraform/sns"
  name   = "miro_image_to_dynamo"
}

module "cold_store_topic" {
  source = "../terraform/sns"
  name   = "miro_cold_store"
}

module "tandem_vault_topic" {
  source = "../terraform/sns"
  name   = "miro_tandem_vault"
}

module "catalogue_api_topic" {
  source = "../terraform/sns"
  name   = "miro_catalogue_api"
}

module "digital_library_topic" {
  source = "../terraform/sns"
  name   = "miro_digital_library"
}

module "none_topic" {
  source = "../terraform/sns"
  name   = "miro_none"
}
