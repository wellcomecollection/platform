locals {
  loris_image = "${module.images.services["loris"]}"
  nginx_image = "${module.images.services["nginx_loris-delta"]}"
}

module "images" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/images?ref=v19.8.0"

  project = "loris"
  label   = "latest"

  services = [
    "loris",
    "nginx_loris-delta",
  ]
}
