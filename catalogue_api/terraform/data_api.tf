locals {
  prod_es_config = "${var.production_api == "romulus" ? var.es_config_romulus : var.es_config_remus}"
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
