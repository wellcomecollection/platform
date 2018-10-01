
module "service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/default?ref=v11.4.1"

  service_name       = "${var.name}"
  task_desired_count = "1"

  task_definition_arn = "${module.task.task_definition_arn}"

  security_group_ids = ["${var.security_group_ids}"]

  container_port = "${module.task.task_port}"

  ecs_cluster_id = "${var.cluster_id}"

  cpu    = 2048
  memory = 4096

  env_vars        = "${var.env_vars}"
  env_vars_length = "${var.env_vars_length}"

  vpc_id  = "${var.vpc_id}"
  subnets = "${var.private_subnets}"

  namespace_id = "${var.namespace_id}"

  launch_type = "FARGATE"
}

module "task" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/task/prebuilt/single_container?ref=v11.4.1"

  aws_region = "${var.aws_region}"
  task_name  = "${var.name}"

  container_image = "${var.container_image}"
  container_port  = "${var.container_port}"
}