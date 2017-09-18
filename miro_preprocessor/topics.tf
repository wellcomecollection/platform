module "cold_store_topic" {
  source = "../terraform/sns"
  name   = "miro_cold_store"
}

module "tandem_vault_topic" {
  source = "../terraform/sns"
  name   = "miro_tandem_vault"
}

module "digital_library_topic" {
  source = "../terraform/sns"
  name   = "miro_digital_library"
}
