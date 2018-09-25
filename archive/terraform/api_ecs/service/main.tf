resource "aws_alb_listener_rule" "path_rule" {
  listener_arn = "${var.lb_listener_arn}"

  action {
    type             = "forward"
    target_group_arn = "${module.service.target_group_arn}"
  }

  condition {
    field  = "path-pattern"
    values = ["/storage/v1/*"]
  }
}

module "service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/load_balanced?ref=v11.8.0"

  service_name       = "${var.namespace}"
  task_desired_count = "1"

  task_definition_arn = "${module.task.task_definition_arn}"

  security_group_ids = ["${var.service_lb_security_group_id}"]

  container_name = "${module.task.task_name}"
  container_port = "${module.task.task_port}"

  ecs_cluster_id = "${var.ecs_cluster_id}"

  vpc_id  = "${var.vpc_id}"
  subnets = "${var.subnets}"

  namespace_id     = "${var.service_discovery_namespace}"
  healthcheck_path = "${var.health_check_path}"

  launch_type = "FARGATE"
}

module "task" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/task/prebuilt/single_container?ref=v11.8.0"

  aws_region = "${var.aws_region}"
  task_name  = "${var.namespace}"

  container_image = "${var.api_container_image}"
  container_port  = "${var.api_container_port}"
  cpu             = "${var.api_cpu}"
  memory          = "${var.api_memory}"
  env_vars        = "${var.api_env_vars}"
  env_vars_length = "${var.api_env_vars_length}"

  cpu    = "${var.task_cpu}"
  memory = "${var.task_memory}"
}
