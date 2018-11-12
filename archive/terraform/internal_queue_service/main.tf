data "aws_ecs_cluster" "cluster" {
  cluster_name = "${var.cluster_name}"
}

module "service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/sqs_scaling?ref=v11.8.1"

  service_name = "${var.service_name}"

  container_image = "${var.container_image}"

  security_group_ids = "${local.security_group_ids}"

  source_queue_name = "${var.source_queue_name}"
  source_queue_arn  = "${var.source_queue_arn}"

  ecs_cluster_id   = "${data.aws_ecs_cluster.cluster.id}"
  ecs_cluster_name = "${var.cluster_name}"

  cpu    = 512
  memory = 1024

  env_vars        = "${var.env_vars}"
  env_vars_length = "${var.env_vars_length}"

  aws_region = "${var.aws_region}"
  vpc_id     = "${var.vpc_id}"
  subnets    = "${var.subnets}"

  namespace_id = "${var.namespace_id}"

  launch_type = "FARGATE"

  task_desired_count = "${var.desired_task_count}"

  min_capacity = "${var.min_capacity}"
  max_capacity = "${var.max_capacity}"
}

locals {
  security_group_ids = "${concat(list(var.service_egress_security_group_id), var.security_group_ids)}"
}
