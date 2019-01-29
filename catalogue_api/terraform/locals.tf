locals {
  api_release_uri                = "${data.aws_ssm_parameter.api_release_uri.value}"
  api_nginx_release_uri          = "${data.aws_ssm_parameter.api_nginx_release_uri.value}"
  snapshot_generator_release_uri = "${data.aws_ssm_parameter.snapshot_generator_release_uri.value}"
  update_api_docs_release_uri    = "${data.aws_ssm_parameter.update_api_docs_release_uri.value}"

  # API pins

  production_api = "romulus"

  pinned_nginx       = "760097843905.dkr.ecr.eu-west-1.amazonaws.com/uk.ac.wellcome/nginx_api-gw:bad0dbfa548874938d16496e313b05adb71268b7"
  pinned_remus_api   = ""
  pinned_romulus_api = "760097843905.dkr.ecr.eu-west-1.amazonaws.com/uk.ac.wellcome/api:bc67ea53369f7255ffca55e72f04d19102e8c419"

  romulus_es_cluster_credentials = "${local.es_cluster_credentials}"
  remus_es_cluster_credentials   = "${local.es_cluster_credentials}"

  romulus_es_config = {
    index_v1 = "v1-2019-01-24-production-changes"
    index_v2 = "v2-2019-01-24-production-changes"
    doc_type = "work"
  }

  remus_es_config = {
    index_v1 = "v1-2018-12-6-single-shard"
    index_v2 = "v2-2018-12-6-single-shard"
    doc_type = "work"
  }

  # Blue / Green config

  romulus_is_prod     = "${local.production_api == "romulus" ? "true" : "false"}"
  remus_is_prod       = "${local.production_api == "remus" ? "true" : "false"}"
  romulus_app_uri     = "${local.pinned_romulus_api != "" ? local.pinned_romulus_api : local.api_release_uri}"
  remus_app_uri       = "${local.pinned_remus_api != "" ? local.pinned_remus_api : local.api_release_uri}"
  stage_api           = "${local.remus_is_prod == "false" ? "remus" : "romulus"}"
  remus_task_number   = "${local.remus_is_prod == "true" ? 3 : 1}"
  romulus_task_number = "${local.romulus_is_prod == "true" ? 3 : 1}"

  # Catalogue API

  vpc_id                         = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_id}"
  private_subnets                = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_private_subnets}"
  namespace                      = "catalogue-api"
  nginx_container_uri            = "${local.pinned_nginx}"
  gateway_server_error_alarm_arn = "${data.terraform_remote_state.shared_infra.gateway_server_error_alarm_arn}"

  # Data API

  prod_es_config = {
    index_v1 = "${local.romulus_is_prod == "true" ? local.romulus_es_config["index_v1"] : local.remus_es_config["index_v1"]}"
    index_v2 = "${local.romulus_is_prod == "true" ? local.romulus_es_config["index_v2"] : local.remus_es_config["index_v2"]}"
    doc_type = "${local.romulus_is_prod == "true" ? local.romulus_es_config["doc_type"] : local.remus_es_config["doc_type"]}"
  }

  release_id = "${local.romulus_is_prod == "true" ? local.pinned_romulus_api : local.pinned_remus_api}"
}
