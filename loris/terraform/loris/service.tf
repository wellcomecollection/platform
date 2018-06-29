resource "aws_service_discovery_private_dns_namespace" "namespace" {
  name = "${var.namespace}"
  vpc  = "${var.vpc_id}"
}

resource "aws_ecs_cluster" "cluster" {
  name = "${var.namespace}"
}

module "task" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/task/prebuilt/ebs?ref=v11.0.0"

  aws_region = "${var.aws_region}"
  task_name  = "${var.namespace}"

  container_image = "grafana/grafana:5.2.0"
  container_port  = "3000"

  ebs_host_path      = "/ebs/loris"
  ebs_container_path = "/mnt/loris"

  cpu    = 3960
  memory = 7350
}

module "service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/load_balanced?ref=v11.0.0"

  service_name       = "loris"
  task_desired_count = "4"

  security_group_ids = [
    "${aws_security_group.service_lb_security_group.id}",
    "${aws_security_group.service_egress_security_group.id}",
  ]

  deployment_minimum_healthy_percent = "0"
  deployment_maximum_percent         = "200"

  ecs_cluster_id = "${aws_ecs_cluster.cluster.id}"

  vpc_id = "${var.vpc_id}"

  subnets = [
    "${var.private_subnets}",
  ]

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"

  container_port = "${module.task.container_port}"
  container_name = "${module.task.container_name}"

  task_definition_arn = "${module.task.task_definition_arn}"

  healthcheck_path = "/image/"

  launch_type = "EC2"
}
