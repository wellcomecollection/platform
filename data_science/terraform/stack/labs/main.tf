resource "aws_ecs_cluster" "cluster" {
  name = "${var.namespace}"
}

resource "aws_service_discovery_private_dns_namespace" "namespace" {
  name = "${var.namespace}"
  vpc  = "${var.vpc_id}"
}

module "cluster_host" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/ec2/prebuilt/ondemand?ref=v11.8.0"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  vpc_id       = "${var.vpc_id}"

  asg_name                    = "${var.namespace}"
  ssh_ingress_security_groups = []

  subnets  = "${var.private_subnets}"
  key_name = "wellcomedigitalplatform"

  instance_type = "t2.large"

  asg_min     = "1"
  asg_desired = "1"
  asg_max     = "1"
}

module "devise_search_service" {
  source    = "../../modules/service"
  namespace = "${var.namespace}_devise"

  lb_listener_arn    = "${aws_alb_listener.http_80.arn}"
  vpc_id             = "${var.vpc_id}"
  container_image    = "harrisonpim/devise_search:v4"
  ecs_cluster_id     = "${aws_ecs_cluster.cluster.id}"
  vpc_cidr_block     = "${var.vpc_cidr_block}"
  subnets            = "${var.private_subnets}"
  memory             = "5120"
  cpu                = "1024"
  launch_type        = "EC2"
  task_desired_count = 1

  service_discovery_namespace  = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  service_lb_security_group_id = "${aws_security_group.service_lb_security_group.id}"

  application_path  = "/devise"
  health_check_path = "/devise/index.html"
}

module "palette_service" {
  source    = "../../modules/service"
  namespace = "${var.namespace}_palette"

  lb_listener_arn = "${aws_alb_listener.http_80.arn}"
  vpc_id          = "${var.vpc_id}"
  container_image = "harrisonpim/palette:v2"
  ecs_cluster_id  = "${aws_ecs_cluster.cluster.id}"
  vpc_cidr_block  = "${var.vpc_cidr_block}"
  subnets         = "${var.private_subnets}"

  service_discovery_namespace  = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  service_lb_security_group_id = "${aws_security_group.service_lb_security_group.id}"

  application_path  = "/palette"
  health_check_path = "/palette/index.html"
}

module "image_similarity_service" {
  source    = "../../modules/service"
  namespace = "${var.namespace}_iss"

  lb_listener_arn = "${aws_alb_listener.http_80.arn}"
  vpc_id          = "${var.vpc_id}"
  container_image = "harrisonpim/image_similarity:v1"
  ecs_cluster_id  = "${aws_ecs_cluster.cluster.id}"
  vpc_cidr_block  = "${var.vpc_cidr_block}"
  subnets         = "${var.private_subnets}"

  service_discovery_namespace  = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  service_lb_security_group_id = "${aws_security_group.service_lb_security_group.id}"

  application_path  = "/image_similarity"
  health_check_path = "/image_similarity/health_check"
}
