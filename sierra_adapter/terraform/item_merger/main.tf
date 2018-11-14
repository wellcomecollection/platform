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
    windows_queue_url   = "${module.updates_queue.id}"
    metrics_namespace   = "sierra_${local.resource_type_singular}_merger"
    dynamo_table_name   = "${var.merged_dynamo_table_name}"
    bucket_name         = "${var.bucket_name}"
    sierra_items_bucket = "${var.sierra_items_bucket}"
    topic_arn           = "${module.sierra_item_merger_results.arn}"

    # The item merger has to write lots of S3 objects, and we've seen issues
    # where we exhaust the HTTP connection pool.  Turning down the parallelism
    # is an attempt to reduce the number of S3 objects in flight, and avoid
    # these errors.
    sqs_parallelism = 5
  }

  env_vars_length = 7

  aws_region = "${var.aws_region}"
  vpc_id     = "${var.vpc_id}"
  subnets    = ["${var.subnets}"]

  namespace_id = "${var.namespace_id}"

  launch_type = "FARGATE"
}
