data "aws_ecs_cluster" "cluster" {
  cluster_name = "${var.cluster_name}"
}

module "sierra_to_dynamo_service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/sqs_scaling?ref=v11.4.1"

  service_name       = "sierra_items_to_dynamo"
  task_desired_count = "0"

  container_image = "${local.container_image}"

  security_group_ids = [
    "${var.interservice_security_group_id}",
    "${var.service_egress_security_group_id}",
  ]

  source_queue_name = "${module.demultiplexer_queue.name}"
  source_queue_arn  = "${module.demultiplexer_queue.arn}"

  ecs_cluster_id   = "${data.aws_ecs_cluster.cluster.id}"
  ecs_cluster_name = "${var.cluster_name}"

  cpu    = 256
  memory = 512

  env_vars = {
    demultiplexer_queue_url = "${module.demultiplexer_queue.id}"
    metrics_namespace       = "sierra_items_to_dynamo"

    vhs_table_name  = "${local.vhs_sierra_items_table_name}"
    vhs_bucket_name = "${local.vhs_sierra_items_bucket_name}"

    topic_arn = "${module.sierra_to_dynamo_updates_topic.arn}"
  }

  env_vars_length = 5

  aws_region = "${var.aws_region}"
  vpc_id     = "${var.vpc_id}"

  subnets = [
    "${var.subnets}",
  ]

  namespace_id = "${var.namespace_id}"

  launch_type = "FARGATE"
}
