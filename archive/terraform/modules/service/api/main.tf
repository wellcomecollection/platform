module "service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/rest/tcp?ref=v17.1.0"

  vpc_id  = "${var.vpc_id}"
  subnets = ["${var.subnets}"]

  ecs_cluster_id = "${var.cluster_id}"

  service_name = "${var.namespace}"
  namespace_id = "${var.namespace_id}"

  lb_arn        = "${var.lb_arn}"
  listener_port = "${var.listener_port}"

  security_group_ids = ["${var.security_group_ids}"]

  launch_type = "${var.launch_type}"

  task_definition_arn = "${module.task.task_definition_arn}"

  container_port = "${var.nginx_container_port}"
  container_name = "sidecar"
}

module "task" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/task/prebuilt/container_with_sidecar?ref=v17.1.0"

  task_name = "${var.namespace}"

  app_container_image = "${var.container_image}"
  app_container_port  = "${var.container_port}"

  sidecar_container_image = "${var.nginx_container_image}"
  sidecar_container_port  = "${var.nginx_container_port}"

  app_env_vars = "${var.env_vars}"

  app_env_vars_length = "${var.env_vars_length}"

  sidecar_env_vars = {
    APP_HOST = "localhost"
    APP_PORT = "${var.container_port}"
  }

  sidecar_env_vars_length = "2"

  cpu    = "${var.cpu}"
  memory = "${var.memory}"

  sidecar_cpu    = "${var.sidecar_cpu}"
  sidecar_memory = "${var.sidecar_memory}"

  app_cpu    = "${var.app_cpu}"
  app_memory = "${var.app_memory}"

  aws_region = "${var.aws_region}"
}
