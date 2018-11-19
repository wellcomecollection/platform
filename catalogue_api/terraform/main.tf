module "catalogue_api" {
  source = "catalogue_api"

  namespace = "catalogue_api_gw"

  vpc_id  = "${local.catalogue_vpc_delta_id}"
  subnets = ["${local.vpc_delta_private_subnets}"]

  container_port = "8888"

  namespace_id  = "${local.namespace_id}"
  namespace_tld = "${local.namespace_tld}"
  cluster_name  = "${aws_ecs_cluster.cluster.name}"

  es_cluster_credentials = "${var.es_cluster_credentials}"

  romulus_container_image = "${local.romulus_app_uri}"
  remus_container_image   = "${local.remus_app_uri}"
  nginx_container_image   = "${local.nginx_container_uri}"

  romulus_es_config = "${local.es_config_romulus}"
  remus_es_config   = "${local.es_config_remus}"

  production_api = "${local.production_api}"
}

module "data_api" {
  source = "data_api"

  aws_region   = "${var.aws_region}"
  infra_bucket = "${var.infra_bucket}"
  key_name     = "${var.key_name}"

  es_cluster_credentials = "${var.es_cluster_credentials}"

  es_config_snapshot = "${local.prod_es_config}"

  snapshot_generator_release_id = "${local.release_id}"

  critical_slack_webhook = "${var.critical_slack_webhook}"
}

module "api_docs" {
  source = "api_docs"

  container_uri = "${local.update_api_docs_container_uri}"
}