data "aws_ecs_cluster" "cluster" {
  cluster_name = "${var.cluster_name}"
}

module "sierra_merger_service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/sqs_scaling?ref=v11.4.1"

  service_name       = "sierra_${local.resource_type_singular}_merger"
  task_desired_count = "0"

  container_image = "${local.container_image}"

  security_group_ids = [
    "${var.interservice_security_group_id}",
    "${var.service_egress_security_group_id}",
  ]

  source_queue_name = "${module.updates_queue.name}"
  source_queue_arn  = "${module.updates_queue.arn}"

  ecs_cluster_id   = "${data.aws_ecs_cluster.cluster.id}"
  ecs_cluster_name = "${var.cluster_name}"

  cpu    = 256
  memory = 512

  env_vars = {
    windows_queue_url = "${module.updates_queue.id}"
    metrics_namespace = "sierra_${local.resource_type_singular}_merger"
    dynamo_table_name = "${var.merged_dynamo_table_name}"
    bucket_name       = "${var.bucket_name}"
    topic_arn         = "${module.sierra_bib_merger_results.arn}"
  }

  env_vars_length = 5

  aws_region = "${var.aws_region}"
  vpc_id     = "${var.vpc_id}"
  subnets    = ["${var.subnets}"]

  namespace_id = "${var.namespace_id}"

  launch_type = "FARGATE"
}
