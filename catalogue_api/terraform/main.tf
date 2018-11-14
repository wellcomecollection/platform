module "catalogue_api" {
  source = "catalogue_api"

  namespace = "catalogue_api_gw"

  vpc_id  = "${local.vpc_id}"
  subnets = ["${local.private_subnets}"]

  container_port = "8888"

  namespace_id  = "${local.namespace_id}"
  namespace_tld = "${aws_service_discovery_private_dns_namespace.namespace.name}"
  cluster_name  = "${aws_ecs_cluster.cluster.name}"

  es_cluster_credentials = "${var.es_cluster_credentials}"

  romulus_container_image = "${local.romulus_app_uri}"
  remus_container_image   = "${local.remus_app_uri}"
  nginx_container_image   = "${local.nginx_container_uri}"

  romulus_es_config = "${local.es_config_romulus}"
  remus_es_config   = "${local.es_config_remus}"

  production_api = "${local.production_api}"
}
