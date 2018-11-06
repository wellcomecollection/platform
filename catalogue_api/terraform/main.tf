module "catalogue_api" {
  source = "catalogue_api"

  namespace = "test"

  container_image = "${local.romulus_app_uri}"

  namespace_id = "${local.namespace_id}"
  cluster_name = "${aws_ecs_cluster.cluster.name}"
  vpc_id = "${local.vpc_id}"

  subnets = ["${local.private_subnets}"]

  es_cluster_credentials = "${var.es_cluster_credentials}"
  es_config              = "${local.es_config_romulus}"
}