locals {
  # API pins

  production_api       = "romulus"
  pinned_nginx         = "bad0dbfa548874938d16496e313b05adb71268b7"
  pinned_remus_api     = ""
  pinned_remus_nginx   = ""
  pinned_romulus_api   = "5d753c10ca58846ca67df73dd9998771b700757b"
  pinned_romulus_nginx = "3dd8a423123e1d175dd44520fcf03435a5fc92c8"

  # Blue / Green config

  romulus_api_release_id   = "${local.pinned_romulus_api != "" ? local.pinned_romulus_api : var.release_ids["api"]}"
  romulus_nginx_release_id = "${local.pinned_romulus_nginx != "" ? local.pinned_romulus_nginx : var.release_ids["nginx_api-delta"]}"
  remus_api_release_id     = "${local.pinned_remus_api != "" ? local.pinned_remus_api : var.release_ids["api"]}"
  remus_nginx_release_id   = "${local.pinned_remus_nginx != "" ? local.pinned_remus_nginx : var.release_ids["nginx_api-delta"]}"
  romulus_app_uri          = "${module.ecr_repository_api.repository_url}:${local.romulus_api_release_id}"
  remus_app_uri            = "${module.ecr_repository_api.repository_url}:${local.remus_api_release_id}"
  romulus_is_prod          = "${local.production_api == "romulus" ? "true" : "false"}"
  remus_is_prod            = "${local.production_api == "remus" ? "true" : "false"}"
  remus_hostname           = "${local.remus_is_prod == "true" ? var.api_prod_host : var.api_stage_host}"
  romulus_hostname         = "${local.romulus_is_prod == "true" ? var.api_prod_host : var.api_stage_host}"
  remus_task_number        = "${local.remus_is_prod == "true" ? 3 : 1}"
  romulus_task_number      = "${local.romulus_is_prod == "true" ? 3 : 1}"
  remus_enable_alb_alarm   = "${local.remus_is_prod == "true" ? 1 : 0}"
  romulus_enable_alb_alarm = "${local.romulus_is_prod == "true" ? 1 : 0}"
  es_config_romulus = {
    index_v1 = "v1-2018-09-27-marc-610-subjects"
    index_v2 = "v2-2018-09-27-marc-610-subjects"
    doc_type = "work"
  }
  es_config_remus = {
    index_v1 = "v1-2018-09-27-marc-610-subjects"
    index_v2 = "v2-2018-09-27-marc-610-subjects"
    doc_type = "work"
  }

  # Catalogue API

  namespace                               = "catalogue-api"
  vpc_id                                  = "${data.terraform_remote_state.shared_infra.catalogue_vpc_id}"
  public_subnets                          = "${data.terraform_remote_state.shared_infra.catalogue_public_subnets}"
  private_subnets                         = "${data.terraform_remote_state.shared_infra.catalogue_private_subnets}"
  alb_api_wc_service_lb_security_group_id = "${data.terraform_remote_state.infra_critical.alb_api_wc_service_lb_security_group_id}"
  alb_api_wc_https_listener_arn           = "${data.terraform_remote_state.infra_critical.alb_api_wc_https_listener_arn}"
  alb_api_wc_cloudwatch_id                = "${data.terraform_remote_state.infra_critical.alb_api_wc_cloudwatch_id}"
  nginx_container_uri                     = "${module.ecr_repository_nginx_api-gw.repository_url}:${local.pinned_nginx}"
  namespace_id                            = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  namespace_tld                           = "${aws_service_discovery_private_dns_namespace.namespace.name}"
  catalogue_vpc_delta_id                  = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_id}"
  vpc_delta_private_subnets               = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_private_subnets}"

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
