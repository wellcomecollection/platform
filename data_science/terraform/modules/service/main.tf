resource "aws_alb_listener_rule" "path_rule" {
  listener_arn = "${var.lb_listener_arn}"

  action {
    type             = "forward"
    target_group_arn = "${module.service.target_group_arn}"
  }

  condition {
    field  = "path-pattern"
    values = ["/${var.namespace}/*"]
  }
}

module "task" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/task/prebuilt/default?ref=v11.0.0"

  aws_region = "${var.aws_region}"
  task_name  = "${var.namespace}"

  container_image = "${var.container_image}"
  container_port  = "${var.container_port}"

  memory = "${var.memory}"
  cpu    = "${var.cpu}"
}

module "service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/load_balanced?ref=v11.0.0"

  service_name       = "${var.namespace}"
  task_desired_count = "${var.task_desired_count}"

  task_definition_arn = "${module.task.task_definition_arn}"

  security_group_ids = ["${var.service_lb_security_group_id}"]

  container_name = "${module.task.container_name}"
  container_port = "${module.task.container_port}"

  ecs_cluster_id = "${var.ecs_cluster_id}"

  vpc_id  = "${var.vpc_id}"
  subnets = "${var.subnets}"

  namespace_id     = "${var.service_discovery_namespace}"
  healthcheck_path = "${var.health_check_path}"

  deployment_minimum_healthy_percent = "0"

  launch_type = "${var.launch_type}"
}
