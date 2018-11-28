locals {
  # API pins

  production_api     = "romulus"
  pinned_nginx       = "bad0dbfa548874938d16496e313b05adb71268b7"
  pinned_remus_api   = ""
  pinned_romulus_api = "692e6e5b5e6f574cba8bb0a78d849e0350fff2c0"

  # Blue / Green config

  romulus_api_release_id = "${local.pinned_romulus_api != "" ? local.pinned_romulus_api : var.release_ids["api"]}"
  remus_api_release_id   = "${local.pinned_remus_api != "" ? local.pinned_remus_api : var.release_ids["api"]}"
  romulus_app_uri        = "${module.ecr_repository_api.repository_url}:${local.romulus_api_release_id}"
  remus_app_uri          = "${module.ecr_repository_api.repository_url}:${local.remus_api_release_id}"
  romulus_is_prod        = "${local.production_api == "romulus" ? "true" : "false"}"
  remus_is_prod          = "${local.production_api == "remus" ? "true" : "false"}"
  es_config_romulus = {
    index_v1 = "v1-2018-11-27-correct-merge"
    index_v2 = "v2-2018-11-27-correct-merge"
    doc_type = "work"
  }
  es_config_remus = {
    index_v1 = "v1-2018-11-27-correct-merge"
    index_v2 = "v2-2018-11-27-correct-merge"
    doc_type = "work"
  }

  # Catalogue API

  vpc_id              = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_id}"
  private_subnets     = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_private_subnets}"
  namespace           = "catalogue-api"
  nginx_container_uri = "${module.ecr_repository_nginx_api-gw.repository_url}:${local.pinned_nginx}"

  # Data API

  prod_es_config = {
    index_v1 = "${local.romulus_is_prod == "true" ? local.es_config_romulus["index_v1"] : local.es_config_remus["index_v1"]}"
    index_v2 = "${local.romulus_is_prod == "true" ? local.es_config_romulus["index_v2"] : local.es_config_remus["index_v2"]}"
    doc_type = "${local.romulus_is_prod == "true" ? local.es_config_romulus["doc_type"] : local.es_config_remus["doc_type"]}"
  }
  release_id = "${local.romulus_is_prod == "true" ? local.pinned_romulus_api : local.pinned_remus_api}"

  # Update API docs

  update_api_docs_container_uri = "${module.ecr_repository_update_api_docs.repository_url}:${var.release_ids["update_api_docs"]}"
}
