locals {
  api_image = "${module.images.services["api"]}"
  nginx_image     = "${module.images.services["nginx_api-gw"]}"

  snapshot_generator_image = "${module.images.services["snapshot_generator"]}"
  update_api_docs_image    = "${module.images.services["update_api_docs"]}"
}

module "images" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/images?ref=v19.8.0"

  project  = "catalogue_api"
  label    = "latest"

  services = [
    "api",
    "snapshot_generator",
    "update_api_docs",
    "nginx_api-gw"
  ]
}