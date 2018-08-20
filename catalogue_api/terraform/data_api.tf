locals {
  prod_es_config = {
    index_v1 = "${var.production_api == "romulus" ? var.es_config_romulus["index_v1"] : var.es_config_remus["index_v1"]}"
    index_v2 = "${var.production_api == "romulus" ? var.es_config_romulus["index_v2"] : var.es_config_remus["index_v2"]}"
    doc_type = "${var.production_api == "romulus" ? var.es_config_romulus["doc_type"] : var.es_config_remus["doc_type"]}"
  }
}

module "data_api" {
  source = "./data_api"

  aws_region   = "${var.aws_region}"
  infra_bucket = "${var.infra_bucket}"
  release_ids  = "${var.release_ids}"
  key_name     = "${var.key_name}"

  data_acm_cert_arn = "${var.data_acm_cert_arn}"

  es_cluster_credentials = "${var.es_cluster_credentials}"

  es_config_snapshot = "${local.prod_es_config}"
}
