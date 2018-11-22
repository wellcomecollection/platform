locals {
  remus_listener_port   = "80"
  romulus_listener_port = "8080"

  nginx_release_id = "8322c88784d2dd40de270fe7d0c456fc528669a4"

  stage_listener_port = "${var.production_api == "remus" ? local.romulus_listener_port : local.remus_listener_port}"
  prod_listener_port  = "${var.production_api == "remus" ? local.remus_listener_port : local.romulus_listener_port}"

  romulus_is_prod = "${var.production_api == "romulus" ? "true" : "false"}"
  remus_is_prod   = "${var.production_api == "remus" ? "true" : "false"}"

  remus_task_number   = "${local.remus_is_prod == "true" ? 3 : 1}"
  romulus_task_number = "${local.romulus_is_prod == "true" ? 3 : 1}"
}
