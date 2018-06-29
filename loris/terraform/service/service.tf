resource "aws_service_discovery_private_dns_namespace" "namespace" {
  name = "${var.namespace}"
  vpc  = "${var.vpc_id}"
}

resource "aws_ecs_cluster" "cluster" {
  name = "${var.namespace}"
}

module "task" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/task/prebuilt/container_with_sidecar+ebs?ref=task-defs-with-sidecar"

  aws_region = "${var.aws_region}"
  task_name  = "${var.namespace}"

  cpu    = "${var.cpu}"
  memory = "${var.memory}"

  log_group_prefix = "${var.log_group_prefix}"

  app_container_image = "${var.app_container_image}"
  app_container_port  = "${var.app_container_port}"
  app_cpu             = "${var.app_cpu}"
  app_memory          = "${var.app_memory}"
  app_env_vars        = "${var.app_env_vars}"

  sidecar_container_image = "${var.sidecar_container_image}"
  sidecar_container_port  = "${var.sidecar_container_port}"
  sidecar_cpu             = "${var.sidecar_cpu}"
  sidecar_memory          = "${var.sidecar_memory}"
  sidecar_env_vars        = "${var.sidecar_env_vars}"

  ebs_host_path      = "/ebs/loris"
  ebs_container_path = "${var.ebs_container_path}"
}

module "service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/load_balanced?task-defs-with-sidecar"

  service_name       = "${var.namespace}"
  task_desired_count = "${var.task_desired_count}"

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

  container_port = "${module.task.sidecar_container_port}"
  container_name = "${module.task.sidecar_task_name}"

  task_definition_arn = "${module.task.task_definition_arn}"

  healthcheck_path = "${var.healthcheck_path}"

  launch_type = "EC2"
}
