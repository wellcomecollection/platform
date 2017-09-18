module "miro_image_to_dynamo_topic" {
  source = "../terraform/sns"
  name   = "miro_image_to_dynamo_topic"
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
