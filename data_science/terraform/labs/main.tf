resource "aws_ecs_cluster" "cluster" {
  name = "${var.namespace}"
}

resource "aws_service_discovery_private_dns_namespace" "namespace" {
  name = "${var.namespace}"
  vpc  = "${var.vpc_id}"
}

module "palette_service" {
  source    = "service"
  namespace = "palette"

  lb_listener_arn = "${aws_alb_listener.http_80.arn}"
  vpc_id          = "${var.vpc_id}"
  container_image = "harrisonpim/palette:v2"
  ecs_cluster_id  = "${aws_ecs_cluster.cluster.id}"
  vpc_cidr_block  = "${var.vpc_cidr_block}"
  subnets         = "${var.private_subnets}"

  service_discovery_namespace  = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  service_lb_security_group_id = "${aws_security_group.service_lb_security_group.id}"
  health_check_path            = "/palette/index.html"
}

module "image_similarity_service" {
  source    = "service"
  namespace = "image_similarity"

  lb_listener_arn = "${aws_alb_listener.http_80.arn}"
  vpc_id          = "${var.vpc_id}"
  container_image = "harrisonpim/image_similarity:v1"
  ecs_cluster_id  = "${aws_ecs_cluster.cluster.id}"
  vpc_cidr_block  = "${var.vpc_cidr_block}"
  subnets         = "${var.private_subnets}"

  service_discovery_namespace  = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  service_lb_security_group_id = "${aws_security_group.service_lb_security_group.id}"
  health_check_path            = "/image_similarity/health_check"
}
