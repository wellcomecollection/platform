locals {
  remus_listener_port   = "80"
  romulus_listener_port = "8080"

  nginx_release_id = "8322c88784d2dd40de270fe7d0c456fc528669a4"

  prod_listener_port = "${var.production_api == "remus" ? local.remus_listener_port : local.romulus_listener_port}"
}
