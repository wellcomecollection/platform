data "aws_ecs_cluster" "cluster" {
  cluster_name = "${var.cluster_name}"
}

module "sierra_reader_service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/sqs_scaling?ref=v11.4.1"

  service_name       = "${local.service_name}"
  task_desired_count = "0"

  container_image = "${local.container_image}"

  security_group_ids = [
    "${var.interservice_security_group_id}",
    "${var.service_egress_security_group_id}",
  ]

  cpu    = 256
  memory = 512

  source_queue_name = "${module.windows_queue.name}"
  source_queue_arn  = "${module.windows_queue.arn}"

  ecs_cluster_id   = "${data.aws_ecs_cluster.cluster.id}"
  ecs_cluster_name = "${var.cluster_name}"

  aws_region = "${var.aws_region}"
  vpc_id     = "${var.vpc_id}"

  subnets = [
    "${var.subnets}",
  ]

  namespace_id = "${var.namespace_id}"

  env_vars = {
    resource_type = "${var.resource_type}"

    windows_queue_url = "${module.windows_queue.id}"
    bucket_name       = "${var.bucket_name}"

    metrics_namespace = "${local.service_name}"

    sierra_api_url      = "${var.sierra_api_url}"
    sierra_oauth_key    = "${var.sierra_oauth_key}"
    sierra_oauth_secret = "${var.sierra_oauth_secret}"
    sierra_fields       = "${var.sierra_fields}"

    batch_size = 50
  }

  env_vars_length = 9

  launch_type = "FARGATE"
}
