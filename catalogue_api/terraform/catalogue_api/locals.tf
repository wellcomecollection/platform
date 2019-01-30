locals {
  remus_listener_port   = "80"
  romulus_listener_port = "8080"

  prod_listener_port = "${var.production_api == "remus" ? local.remus_listener_port : local.romulus_listener_port}"
}
