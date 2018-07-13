data "aws_ecs_cluster" "cluster" {
  cluster_name = "${var.cluster_name}"
}

module "service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/sqs_scaling?ref=v11.0.0"

  service_name       = "${var.service_name}"
  task_desired_count = "0"

  container_image = "${var.container_image}"

  security_group_ids = [
    "${var.interservice_security_group_id}",
    "${var.service_egress_security_group_id}",
  ]

  source_queue_name = "${var.source_queue_name}"
  source_queue_arn  = "${var.source_queue_arn}"

  ecs_cluster_id   = "${data.aws_ecs_cluster.cluster.id}"
  ecs_cluster_name = "${var.cluster_name}"

  cpu    = 512
  memory = 2048

  env_vars = "${var.env_vars}"

  aws_region = "${var.aws_region}"
  vpc_id     = "${var.vpc_id}"
  subnets    = ["${var.subnets}"]

  namespace_id = "${var.namespace_id}"

  launch_type = "FARGATE"

  max_capacity = "${var.max_capacity}"
}
