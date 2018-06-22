module "task" {
  source = "../../../../terraform-modules/ecs/modules/task/prebuilt/default"

  aws_region = "${local.aws_region}"
  task_name  = "${local.namespace}"

  container_image = "harrisonpim/palette:v1"
  container_port  = "80"
}

module "service" {
  source = "../../../../terraform-modules/ecs/modules/service/prebuilt/load_balanced"

  service_name       = "${local.namespace}"
  task_desired_count = "1"

  task_definition_arn = "${module.task.task_definition_arn}"

  security_group_ids = ["${aws_security_group.service_lb_security_group.id}"]

  container_name = "${module.task.container_name}"
  container_port = "${module.task.container_port}"

  ecs_cluster_id = "${aws_ecs_cluster.cluster.id}"

  vpc_id  = "${module.network.vpc_id}"
  subnets = "${module.network.private_subnets}"

  namespace_id     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  healthcheck_path = "/static/index.html"

  launch_type = "FARGATE"
}