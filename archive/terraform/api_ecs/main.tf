resource "aws_ecs_cluster" "cluster" {
  name = "${var.namespace}"
}

resource "aws_service_discovery_private_dns_namespace" "namespace" {
  name = "${var.namespace}_api"
  vpc  = "${var.vpc_id}"
}

module "api_ecs" {
  source    = "service"
  namespace = "${var.namespace}_api"

  lb_listener_arn = "${aws_alb_listener.api_https.arn}"
  vpc_id          = "${var.vpc_id}"

  container_image = "${var.archive_api_container_image}"
  container_port  = "${var.archive_api_container_port}"

  env_vars = {
    PROGRESS_MANAGER_ENDPOINT = "http://progress_http.archive-storage:9001"
    BAGS_MANAGER_ENDPOINT     = "http://registrar_http.archive-storage:9001"
  }

  env_vars_length = 4

  service_lb_security_group_id   = "${aws_security_group.service_lb_security_group.id}"
  interservice_security_group_id = "${var.interservice_security_group_id}"
  ecs_cluster_id                 = "${aws_ecs_cluster.cluster.id}"
  subnets                        = "${var.private_subnets}"

  service_discovery_namespace = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  health_check_path           = "${var.api_path}/healthcheck"

  cpu    = 512
  memory = 1024
}
