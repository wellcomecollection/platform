locals {
  infra_bucket = "${data.aws_ssm_parameter.infra_bucket.value}"

  # API pins

  production_api = "remus"
  stage_api      = "${local.remus_is_prod == "false" ? "remus" : "romulus"}"
  pinned_nginx    = "${local.nginx_image}"

  ## Romulus

  romulus_is_prod     = "${local.production_api == "romulus" ? "true" : "false"}"

  pinned_romulus_api = "760097843905.dkr.ecr.eu-west-1.amazonaws.com/uk.ac.wellcome/api:bc67ea53369f7255ffca55e72f04d19102e8c419"

  romulus_es_config = {
    index_v1 = "v1-2019-01-24-production-changes"
    index_v2 = "v2-2019-01-24-production-changes"
    doc_type = "work"
  }

  romulus_task_number = "${local.romulus_is_prod == "true" ? 3 : 1}"
  romulus_app_uri     = "${local.pinned_romulus_api != "" ? local.pinned_romulus_api : local.api_image}"

  ## Remus

  remus_is_prod       = "${local.production_api == "remus" ? "true" : "false"}"

  pinned_remus_api   = "760097843905.dkr.ecr.eu-west-1.amazonaws.com/uk.ac.wellcome/api:dd9a66fa8727cd23486a8306b8946d3a38ada4a1"

  remus_es_config = {
    index_v1 = "v1-2019-01-24-production-changes"
    index_v2 = "v2-2019-02-05-extra-subject-labels"
    doc_type = "work"
  }

  remus_task_number   = "${local.remus_is_prod == "true" ? 3 : 1}"
  remus_app_uri       = "${local.pinned_remus_api != "" ? local.pinned_remus_api : local.api_image}"

  # Amber = the "frozen" V1 API

  v1_amber_es_config = {
    index_v1 = "v1-2019-01-24-production-changes"
    index_v2 = "v2-2019-01-24-production-changes"
    doc_type = "work"
  }
  v1_amber_app_uri     = "760097843905.dkr.ecr.eu-west-1.amazonaws.com/uk.ac.wellcome/api:bc67ea53369f7255ffca55e72f04d19102e8c419"
  v1_amber_task_number = 2

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
