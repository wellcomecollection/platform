data "aws_ecs_cluster" "cluster" {
  cluster_name= "${var.cluster_name}"
}

locals {
  nginx_tcp_security_group = ["${var.nginx_tcp_security_group_id}"]
}

module "service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/default?ref=v11.10.0"

  service_name = "${var.service_name}"
  task_desired_count = "1"

  task_definition_arn = "${module.task.task_definition_arn}"

  security_group_ids = [
    "${var.security_group_ids}"]

  container_port = "${module.task.task_port}"

  ecs_cluster_id = "${data.aws_ecs_cluster.cluster.arn}"

  vpc_id = "${var.vpc_id}"
  subnets = "${var.private_subnets}"

  namespace_id = "${var.namespace_id}"

  launch_type = "FARGATE"
}

module "nginx" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/prebuilt/rest?ref=v14.2.0"

  vpc_id       = "${var.vpc_id}"
  subnets      = ["${var.private_subnets}"]
  cluster_name = "${var.cluster_name}"
  service_name = "${var.service_name}-nginx"
  namespace_id = "${var.namespace_id}"

  container_image = "${var.nginx_container_image}"
  container_port  = "${var.nginx_container_port}"

  env_vars = {
    APP_HOST = "${var.service_name}.${var.namespace}"
    APP_PORT = "${var.container_port}"
  }

  env_vars_length = "2"

  security_group_ids               = ["${concat(var.security_group_ids, local.nginx_tcp_security_group)}"]
  service_egress_security_group_id = "${var.service_egress_security_group_id}"

  target_group_protocol = "TCP"
}

data "aws_lb_target_group" "tcp_target_group" {
  name = "${module.nginx.target_group_name}"
}

module "task" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/task/prebuilt/single_container?ref=v11.9.0"

  cpu    = 512
  memory = 1024

  env_vars        = "${var.env_vars}"
  env_vars_length = "${var.env_vars_length}"
  command         = "${var.command}"

  aws_region = "${var.aws_region}"
  task_name  = "${var.service_name}"

  container_image = "${var.container_image}"
  container_port  = "${var.container_port}"
}
