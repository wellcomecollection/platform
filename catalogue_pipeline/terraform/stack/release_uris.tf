locals {
  id_minter_image = "${module.images.services["id_minter"]}"
  recorder_image  = "${module.images.services["recorder"]}"
  matcher_image   = "${module.images.services["matcher"]}"
  merger_image    = "${module.images.services["merger"]}"
  ingestor_image  = "${module.images.services["ingestor"]}"

  transformer_miro_image   = "${module.images.services["transformer_miro"]}"
  transformer_sierra_image = "${module.images.services["transformer_sierra"]}"
}

module "images" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/images?ref=v19.8.0"

  project = "catalogue_pipeline"
  label   = "${var.release_label}"

  services = [
    "ingestor",
    "matcher",
    "merger",
    "id_minter",
    "recorder",
    "transformer_miro",
    "transformer_sierra",
  ]
}
