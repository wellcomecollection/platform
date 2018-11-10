module "catalogue_api" {
  source = "catalogue_api"

  namespace = "catalogue-api-test-api-gw"

  romulus_container_image = "${local.romulus_app_uri}"
  remus_container_image = "${local.romulus_app_uri}"

  container_port = "8888"

  namespace_id = "${local.namespace_id}"
  cluster_name = "${aws_ecs_cluster.cluster.name}"
  vpc_id = "${local.vpc_id}"

  subnets = ["${local.private_subnets}"]

  es_cluster_credentials = "${var.es_cluster_credentials}"
  es_config              = "${local.es_config_romulus}"

  romulus_es_config = "${local.es_config_romulus}"
  remus_es_config = "${local.es_config_remus}"
}