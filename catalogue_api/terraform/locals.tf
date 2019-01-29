locals {
  # API pins

  production_api = "remus"

  pinned_nginx       = "bad0dbfa548874938d16496e313b05adb71268b7"
  pinned_remus_api   = "966a00af5552b4c6e0b816ba296b7bc4f898206a"
  pinned_romulus_api = ""

  romulus_es_cluster_credentials = "${var.es_cluster_credentials_v6}"
  remus_es_cluster_credentials   = "${var.es_cluster_credentials_v6}"

  romulus_es_config = {
    index_v1 = "v1-2018-12-6-single-shard"
    index_v2 = "v2-2018-12-6-single-shard"
    doc_type = "work"
  }

  remus_es_config = {
    index_v1 = "v1-2018-12-6-single-shard"
    index_v2 = "v2-2018-12-6-single-shard"
    doc_type = "work"
  }

  # Blue / Green config

  romulus_is_prod        = "${local.production_api == "romulus" ? "true" : "false"}"
  remus_is_prod          = "${local.production_api == "remus" ? "true" : "false"}"
  romulus_api_release_id = "${local.pinned_romulus_api != "" ? local.pinned_romulus_api : var.release_ids["api"]}"
  remus_api_release_id   = "${local.pinned_remus_api != "" ? local.pinned_remus_api : var.release_ids["api"]}"
  romulus_app_uri        = "${module.ecr_repository_api.repository_url}:${local.romulus_api_release_id}"
  remus_app_uri          = "${module.ecr_repository_api.repository_url}:${local.remus_api_release_id}"
  stage_api              = "${local.remus_is_prod == "false" ? "remus" : "romulus"}"
  remus_task_number      = "${local.remus_is_prod == "true" ? 3 : 1}"
  romulus_task_number    = "${local.romulus_is_prod == "true" ? 3 : 1}"

  # Catalogue API

  vpc_id                         = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_id}"
  private_subnets                = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_private_subnets}"
  namespace                      = "catalogue-api"
  nginx_container_uri            = "${module.ecr_repository_nginx_api-gw.repository_url}:${local.pinned_nginx}"
  gateway_server_error_alarm_arn = "${data.terraform_remote_state.shared_infra.gateway_server_error_alarm_arn}"

  # Data API

  prod_es_config = {
    index_v1 = "${local.romulus_is_prod == "true" ? local.romulus_es_config["index_v1"] : local.remus_es_config["index_v1"]}"
    index_v2 = "${local.romulus_is_prod == "true" ? local.romulus_es_config["index_v2"] : local.remus_es_config["index_v2"]}"
    doc_type = "${local.romulus_is_prod == "true" ? local.romulus_es_config["doc_type"] : local.remus_es_config["doc_type"]}"
  }
  release_id = "${local.romulus_is_prod == "true" ? local.pinned_romulus_api : local.pinned_remus_api}"

  # Update API docs

  update_api_docs_container_uri = "${module.ecr_repository_update_api_docs.repository_url}:${var.release_ids["update_api_docs"]}"
}
