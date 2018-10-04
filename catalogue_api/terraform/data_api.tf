locals {
  prod_es_config = {
    index_v1 = "${local.romulus_is_prod ? var.es_config_romulus["index_v1"] : var.es_config_remus["index_v1"]}"
    index_v2 = "${local.romulus_is_prod ? var.es_config_romulus["index_v2"] : var.es_config_remus["index_v2"]}"
    doc_type = "${local.romulus_is_prod ? var.es_config_romulus["doc_type"] : var.es_config_remus["doc_type"]}"
  }

  release_id = "${local.romulus_is_prod ? local.pinned_romulus_api : local.pinned_remus_api}"
}

module "data_api" {
  source = "./data_api"

  aws_region   = "${var.aws_region}"
  infra_bucket = "${var.infra_bucket}"
  key_name     = "${var.key_name}"

  es_cluster_credentials = "${var.es_cluster_credentials}"

  es_config_snapshot = "${local.prod_es_config}"

  snapshot_generator_release_id = "${local.release_id}"
}
