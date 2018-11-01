locals {
  # In an ideal world we'd be able to pass through the map directly.  Currently
  # Terraform can't do that, and if you try you get an error:
  #
  #     conditional operator cannot be used with map values
  #
  # It sounds like we might be getting it in 0.12:
  # https://github.com/hashicorp/terraform/issues/12453#issuecomment-433555953
  #
  # If so, we should come back and clean this up!
  #
  prod_es_config = {
    index_v1 = "${local.romulus_is_prod ? local.es_config_romulus["index_v1"] : local.es_config_remus["index_v1"]}"
    index_v2 = "${local.romulus_is_prod ? local.es_config_romulus["index_v2"] : local.es_config_remus["index_v2"]}"
    doc_type = "${local.romulus_is_prod ? local.es_config_romulus["doc_type"] : local.es_config_remus["doc_type"]}"
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

  critical_slack_webhook = "${var.critical_slack_webhook}"
}
