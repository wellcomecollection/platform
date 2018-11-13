locals {
  remus_listener_port   = "80"
  romulus_listener_port = "8080"

  stage_listener_port = "${var.production_api == "remus" ? local.romulus_listener_port : local.remus_listener_port}"
  prod_listener_port  = "${var.production_api == "remus" ? local.remus_listener_port : local.romulus_listener_port}"
}
